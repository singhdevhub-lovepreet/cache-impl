package cache.expense;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/parse")
    public ResponseEntity<Expense> parseExpense(@RequestBody Map<String, String> request) {
        String rawExpense = request.get("expense");
        String provider = request.getOrDefault("provider", "claude");

        Expense parsed = expenseService.parseExpense(rawExpense, provider);
        return ResponseEntity.ok(parsed);
    }
}
