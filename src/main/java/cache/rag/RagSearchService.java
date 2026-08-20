package cache.rag;

import cache.faq.EmbeddingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Query-side of the pipeline: build a metadata pre-filter, encode the query into dense +
 * sparse vectors, and run the hybrid RRF search.
 * <p>
 * The pre-filter is the key to avoiding "search the whole vector DB" noise: by matching on
 * {@code name}, {@code keyword} and {@code createdAt} we restrict the ANN/BM25 search to a
 * smaller relevant slice of the collection.
 */
@Service
public class RagSearchService {

    private static final int DEFAULT_TOP_K = 5;

    private final EmbeddingService embeddingService;
    private final Bm25SparseEncoder sparseEncoder;
    private final RagQdrantService qdrantService;

    public RagSearchService(EmbeddingService embeddingService,
                            Bm25SparseEncoder sparseEncoder,
                            RagQdrantService qdrantService) {
        this.embeddingService = embeddingService;
        this.sparseEncoder = sparseEncoder;
        this.qdrantService = qdrantService;
    }

    public List<RagSearchResult> search(SearchRequest request) {
        List<Double> denseQuery = embeddingService.embed(request.getQuery());
        SparseVector sparseQuery = sparseEncoder.encodeQuery(request.getQuery());
        Map<String, Object> filter = buildFilter(request);
        int topK = request.getTopK() == null ? DEFAULT_TOP_K : request.getTopK();
        return qdrantService.hybridSearch(denseQuery, sparseQuery, filter, topK);
    }

    /**
     * Translate the request's optional metadata into a Qdrant filter. Returns {@code null}
     * when no filters are supplied (search the whole collection).
     */
    private Map<String, Object> buildFilter(SearchRequest request) {
        List<Map<String, Object>> must = new ArrayList<>();

        if (request.getName() != null && !request.getName().isBlank()) {
            must.add(Map.of("key", "name", "match", Map.of("value", request.getName())));
        }
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            // Matching a single value against an array payload succeeds if the array contains it.
            must.add(Map.of("key", "keywords", "match", Map.of("value", request.getKeyword().toLowerCase())));
        }
        if (request.getCreatedAfter() != null) {
            must.add(Map.of("key", "createdAt", "range", Map.of("gte", request.getCreatedAfter())));
        }

        return must.isEmpty() ? null : Map.of("must", must);
    }
}
