package cache.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pipeline stage: Indexing + hybrid retrieval against Qdrant.
 * <p>
 * The collection stores two named vectors per chunk:
 * <ul>
 *   <li>{@code dense} - 1536-dim OpenAI embedding, Cosine distance (semantic search)</li>
 *   <li>{@code sparse} - BM25 sparse vector with the {@code idf} modifier (lexical search)</li>
 * </ul>
 * Retrieval uses the Query API: a metadata {@code filter} shrinks the candidate set, two
 * prefetches (dense + sparse) run over that slice, and Reciprocal Rank Fusion combines them.
 */
@Service
public class RagQdrantService {

    private static final Logger log = LoggerFactory.getLogger(RagQdrantService.class);

    static final String DENSE_VECTOR = "dense";
    static final String SPARSE_VECTOR = "sparse";

    private final RestClient restClient;
    private final String collectionName;
    private final int prefetchLimit;

    public RagQdrantService(@Value("${qdrant.url}") String qdrantUrl,
                            @Value("${qdrant.api-key}") String apiKey,
                            @Value("${rag.qdrant.collection-name}") String collectionName,
                            @Value("${rag.search.prefetch-limit}") int prefetchLimit) {
        this.restClient = RestClient.builder()
                .baseUrl(qdrantUrl)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.collectionName = collectionName;
        this.prefetchLimit = prefetchLimit;
    }

    /**
     * Create the collection with a named dense vector and a named sparse vector.
     * The sparse vector enables the {@code idf} modifier so Qdrant applies BM25 IDF
     * weighting server-side.
     */
    public void createCollectionIfNotExists(int vectorSize) {
        try {
            restClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .body(Map.class);
            log.info("RAG collection '{}' already exists", collectionName);
        } catch (Exception e) {
            log.info("Creating RAG collection '{}'", collectionName);
            Map<String, Object> body = Map.of(
                    "vectors", Map.of(
                            DENSE_VECTOR, Map.of("size", vectorSize, "distance", "Cosine")
                    ),
                    "sparse_vectors", Map.of(
                            SPARSE_VECTOR, Map.of("modifier", "idf")
                    )
            );
            restClient.put()
                    .uri("/collections/{name}", collectionName)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            log.info("RAG collection '{}' created (dense={} dims, sparse=BM25/idf)", collectionName, vectorSize);
        }
        ensurePayloadIndexes();
    }

    /**
     * Qdrant requires a payload index on any field used in a filter. Create indexes for the
     * metadata fields our hybrid search pre-filters on. Creating an existing index is a no-op,
     * so this is safe to call on every startup/ingest.
     */
    private void ensurePayloadIndexes() {
        createPayloadIndex("name", "keyword");
        createPayloadIndex("keywords", "keyword");
        createPayloadIndex("topic", "keyword");
        createPayloadIndex("createdAt", "integer");
    }

    private void createPayloadIndex(String field, String schema) {
        try {
            restClient.put()
                    .uri("/collections/{name}/index?wait=true", collectionName)
                    .body(Map.of("field_name", field, "field_schema", schema))
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.warn("Could not ensure payload index on '{}' ({})", field, e.getMessage());
        }
    }

    /** Upsert one point per chunk carrying both the dense and sparse vectors plus payload. */
    public void upsertChunks(List<DocumentChunk> chunks,
                             List<List<Double>> denseVectors,
                             List<SparseVector> sparseVectors) {
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            Map<String, Object> payload = new LinkedHashMap<>(chunk.getMetadata());
            payload.put("text", chunk.getText());

            points.add(Map.of(
                    "id", UUID.randomUUID().toString(),
                    "vector", Map.of(
                            DENSE_VECTOR, denseVectors.get(i),
                            SPARSE_VECTOR, sparseVectors.get(i)
                    ),
                    "payload", payload
            ));
        }

        restClient.put()
                .uri("/collections/{name}/points?wait=true", collectionName)
                .body(Map.of("points", points))
                .retrieve()
                .body(Map.class);
        log.info("Upserted {} chunks into RAG collection '{}'", chunks.size(), collectionName);
    }

    /**
     * Hybrid search: pre-filter by metadata, prefetch dense + sparse candidates, fuse with RRF.
     */
    @SuppressWarnings("unchecked")
    public List<RagSearchResult> hybridSearch(List<Double> denseQuery,
                                              SparseVector sparseQuery,
                                              Map<String, Object> filter,
                                              int topK) {
        Map<String, Object> densePrefetch = new LinkedHashMap<>();
        densePrefetch.put("query", denseQuery);
        densePrefetch.put("using", DENSE_VECTOR);
        densePrefetch.put("limit", prefetchLimit);

        Map<String, Object> sparsePrefetch = new LinkedHashMap<>();
        sparsePrefetch.put("query", sparseQuery);
        sparsePrefetch.put("using", SPARSE_VECTOR);
        sparsePrefetch.put("limit", prefetchLimit);

        if (filter != null) {
            densePrefetch.put("filter", filter);
            sparsePrefetch.put("filter", filter);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prefetch", List.of(densePrefetch, sparsePrefetch));
        body.put("query", Map.of("fusion", "rrf"));
        body.put("limit", topK);
        body.put("with_payload", true);

        Map<String, Object> response = restClient.post()
                .uri("/collections/{name}/points/query", collectionName)
                .body(body)
                .retrieve()
                .body(Map.class);

        Map<String, Object> result = (Map<String, Object>) response.get("result");
        List<Map<String, Object>> points = (List<Map<String, Object>>) result.get("points");

        List<RagSearchResult> results = new ArrayList<>();
        for (Map<String, Object> point : points) {
            Map<String, Object> payload = (Map<String, Object>) point.get("payload");
            double score = ((Number) point.get("score")).doubleValue();
            results.add(new RagSearchResult(
                    (String) payload.get("name"),
                    payload.get("chunkIndex") == null ? -1 : ((Number) payload.get("chunkIndex")).intValue(),
                    (String) payload.get("section"),
                    (String) payload.get("text"),
                    score,
                    payload
            ));
        }
        return results;
    }
}
