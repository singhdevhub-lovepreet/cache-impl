package cache.expense;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExpenseService {

    private final LlmService claudeLlmService;
    private final LlmService openAiLlmService;

    @Value("${llm.claude.default-model}")
    private String claudeDefaultModel;

    @Value("${llm.openai.default-model}")
    private String openAiDefaultModel;

    public ExpenseService(
            @Qualifier("claudeLlmService") LlmService claudeLlmService,
            @Qualifier("openAiLlmService") LlmService openAiLlmService) {
        this.claudeLlmService = claudeLlmService;
        this.openAiLlmService = openAiLlmService;
    }

    private static final String SYSTEM_PROMPT = """
            You are an expense parser. Given a raw expense description, extract the structured fields: \
            the expense description, currency code, monetary amount, and merchant name. \
            If any field is unclear, make your best guess from context.""";

    public Expense parseExpense(String rawExpense, String provider) {
        return switch (provider.toLowerCase()) {
            case "claude" -> claudeLlmService.getStructuredOutput(
                    claudeDefaultModel, SYSTEM_PROMPT, rawExpense, Expense.class);
            case "openai" -> openAiLlmService.getStructuredOutput(
                    openAiDefaultModel, SYSTEM_PROMPT, rawExpense, Expense.class);
            default -> throw new IllegalArgumentException("Unknown provider: " + provider + ". Use 'claude' or 'openai'.");
        };
    }
}
