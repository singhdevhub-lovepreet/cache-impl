package cache.expense;

public interface LlmService {

    <T> T getStructuredOutput(String model, String systemPrompt, String userMessage, Class<T> responseType);
}
