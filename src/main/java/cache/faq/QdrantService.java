package cache.faq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QdrantService {

    private static final Logger log = LoggerFactory.getLogger(QdrantService.class);

    private final RestClient restClient;

    @Value("${qdrant.collection-name}")
    private String collectionName;

    public QdrantService(@Value("${qdrant.url}") String qdrantUrl,
                         @Value("${qdrant.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(qdrantUrl)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Create the collection if it doesn't already exist.
     * Uses 1536 dimensions (OpenAI text-embedding-3-small) with cosine distance.
     */
    public void createCollectionIfNotExists(int vectorSize) {
        try {
            // Check if collection exists
            restClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .body(Map.class);
            log.info("Collection '{}' already exists", collectionName);
        } catch (Exception e) {
            // Collection doesn't exist, create it
            log.info("Creating collection '{}'", collectionName);
            Map<String, Object> body = Map.of(
                    "vectors", Map.of(
                            "size", vectorSize,
                            "distance", "Cosine"
                    )
            );
            restClient.put()
                    .uri("/collections/{name}", collectionName)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            log.info("Collection '{}' created", collectionName);
        }
    }

    /**
     * Upsert a batch of FAQ points into the collection.
     */
    public void upsertFaqs(List<Faq> faqs, List<List<Double>> embeddings) {
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < faqs.size(); i++) {
            Faq faq = faqs.get(i);
            points.add(Map.of(
                    "id", UUID.randomUUID().toString(),
                    "vector", embeddings.get(i),
                    "payload", Map.of(
                            "question", faq.getQuestion(),
                            "answer", faq.getAnswer()
                    )
            ));
        }

        Map<String, Object> body = Map.of("points", points);
        restClient.put()
                .uri("/collections/{name}/points", collectionName)
                .body(body)
                .retrieve()
                .body(Map.class);
        log.info("Upserted {} FAQs into collection '{}'", faqs.size(), collectionName);
    }

    /**
     * Search for the top-k most similar FAQs given a query vector.
     */
    public List<FaqSearchResult> search(List<Double> queryVector, int topK) {
        Map<String, Object> body = Map.of(
                "vector", queryVector,
                "limit", topK,
                "with_payload", true
        );

        Map response = restClient.post()
                .uri("/collections/{name}/points/search", collectionName)
                .body(body)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("result");
        List<FaqSearchResult> searchResults = new ArrayList<>();
        for (Map<String, Object> result : results) {
            Map<String, Object> payload = (Map<String, Object>) result.get("payload");
            double score = ((Number) result.get("score")).doubleValue();
            searchResults.add(new FaqSearchResult(
                    (String) payload.get("question"),
                    (String) payload.get("answer"),
                    score
            ));
        }
        return searchResults;
    }
}
