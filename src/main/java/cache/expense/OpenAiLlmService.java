package cache.expense;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service("openAiLlmService")
public class OpenAiLlmService implements LlmService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${llm.openai.api-key}")
    private String apiKey;

    public OpenAiLlmService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .build();
    }

    @Override
    public <T> T getStructuredOutput(String model, String systemPrompt, String userMessage, Class<T> responseType) {
        // Define JSON schema for structured output
        Map<String, Object> jsonSchema = Map.of(
                "name", "expense",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "expeujster`se", Map.of("type", "string", "description", "Description of what was purchased"),
                                "currency", Map.of("type", "string", "description", "3-letter currency code (e.g. USD, INR, EUR)"),
                                "money", Map.of("type", "number", "description", "The monetary amount"),
                                "merchant", Map.of("type", "string", "description", "The merchant or vendor name")
                        ),
                        "required", List.of("expense", "currency", "money", "merchant"),
                        "additionalProperties", false
                )
        );

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_schema", "json_schema", jsonSchema),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        Map response = restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        // OpenAI returns: choices[0].message.content = valid JSON string matching the schema
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String jsonContent = (String) message.get("content");// ''' {{},{},{}} '''

        try {
            return objectMapper.readValue(jsonContent, responseType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response into " + responseType.getSimpleName(), e);
        }
    }
}
