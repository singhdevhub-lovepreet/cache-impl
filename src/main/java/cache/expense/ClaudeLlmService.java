package cache.expense;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service("claudeLlmService")
public class ClaudeLlmService implements LlmService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${llm.claude.api-key}")
    private String apiKey;

    public ClaudeLlmService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .build();
    }

    @Override
    public <T> T getStructuredOutput(String model, String systemPrompt, String userMessage, Class<T> responseType) {
        // Define a tool whose input_schema matches the target POJO
        Map<String, Object> tool = Map.of(
                "name", "extract_structured_data",
                "description", "Extract structured data from the user's input",
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "expense", Map.of("type", "string", "description", "Description of what was purchased"),
                                "currency", Map.of("type", "string", "description", "3-letter currency code (e.g. USD, INR, EUR)"),
                                "money", Map.of("type", "number", "description", "The monetary amount"),
                                "merchant", Map.of("type", "string", "description", "The merchant or vendor name")
                        ),
                        "required", List.of("expense", "currency", "money", "merchant")
                )
        );

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 1024,
                "system", systemPrompt,
                "tools", List.of(tool),
                "tool_choice", Map.of("type", "tool", "name", "extract_structured_data"),
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        Map response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        // Claude returns: content[0] = { type: "tool_use", input: { expense: "...", ... } }
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        Map<String, Object> toolUseBlock = content.stream()
                .filter(block -> "tool_use".equals(block.get("type")))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No tool_use block in Claude response"));

        // input is already a structured Map — convert directly to POJO
        Object input = toolUseBlock.get("input");
        return objectMapper.convertValue(input, responseType);
    }
}
