package cache.faq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dense embeddings backed by the OpenAI embeddings API.
 * Active when {@code embedding.provider=openai}.
 */
@Service("openAiEmbeddingService")
@ConditionalOnProperty(name = "embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingService implements EmbeddingService {

    private final RestClient restClient;

    @Value("${llm.openai.api-key}")
    private String apiKey;

    @Value("${llm.openai.embedding-model}")
    private String embeddingModel;

    public OpenAiEmbeddingService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Double> embed(String text) {
        Map<String, Object> requestBody = Map.of(
                "model", embeddingModel,
                "input", text
        );

        Map response = restClient.post()
                .uri("/v1/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        return toDoubleList(data.get(0).get("embedding"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<List<Double>> embedBatch(List<String> texts) {
        Map<String, Object> requestBody = Map.of(
                "model", embeddingModel,
                "input", texts
        );

        Map response = restClient.post()
                .uri("/v1/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        List<List<Double>> embeddings = new ArrayList<>();
        for (Map<String, Object> item : data) {
            embeddings.add(toDoubleList(item.get("embedding")));
        }
        return embeddings;
    }

    @SuppressWarnings("unchecked")
    private List<Double> toDoubleList(Object embedding) {
        List<Number> numbers = (List<Number>) embedding;
        return numbers.stream().map(Number::doubleValue).toList();
    }
}
