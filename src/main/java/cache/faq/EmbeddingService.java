package cache.faq;

import java.util.List;

/**
 * Dense embedding provider. Implementations are selected at startup via the
 * {@code embedding.provider} property (see {@code application.properties}):
 * <ul>
 *   <li>{@code ollama} - {@link OllamaEmbeddingService} (free, local)</li>
 *   <li>{@code openai} - {@link OpenAiEmbeddingService}</li>
 * </ul>
 */
public interface EmbeddingService {

    /** Generate an embedding for a single text input. */
    List<Double> embed(String text);

    /** Generate embeddings for multiple texts in a single call. */
    List<List<Double>> embedBatch(List<String> texts);
}
