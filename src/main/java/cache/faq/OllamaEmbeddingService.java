package cache.faq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Dense embeddings backed by a local Ollama instance (free, no API key).
 * Active when {@code embedding.provider=ollama} (the default).
 * <p>
 * Uses Ollama's {@code /api/embed} endpoint, which accepts either a single string or a
 * list of strings in {@code input} and returns {@code embeddings} as a list of vectors.
 * Default model is {@code nomic-embed-text} (768 dimensions).
 */
@Service("ollamaEmbeddingService")
@ConditionalOnProperty(name = "embedding.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaEmbeddingService implements EmbeddingService {

    private final RestClient restClient;
    private final String embeddingModel;

    public OllamaEmbeddingService(@Value("${llm.ollama.base-url}") String baseUrl,
                                  @Value("${llm.ollama.embedding-model}") String embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public List<Double> embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<List<Double>> embedBatch(List<String> texts) {
        Map<String, Object> requestBody = Map.of(
                "model", embeddingModel,
                "input", texts
        );

        Map<String, Object> response = restClient.post()
                .uri("/api/embed")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        List<List<Number>> embeddings = (List<List<Number>>) response.get("embeddings");
        if (embeddings == null || embeddings.isEmpty()) {
            throw new IllegalStateException("Ollama returned no embeddings; is the model '"
                    + embeddingModel + "' pulled? Run: ollama pull " + embeddingModel);
        }
        return embeddings.stream()
                .map(v -> v.stream().map(Number::doubleValue).toList())
                .toList();
    }
}
