package cache.async;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/async-demo")
@RequiredArgsConstructor
public class AsyncDemoController {

    private final AsyncUserService asyncUserService;

    @GetMapping("/sync")
    public Map<String, Object> runSync() {
        long start = System.currentTimeMillis();
        String userName = "DemoUser";

        String emailResult = asyncUserService.sendWelcomeEmailSync(userName);
        String auditResult = asyncUserService.writeAuditLogSync(userName);
        String syncResult = asyncUserService.syncToExternalSystemSync(userName);

        long totalTime = System.currentTimeMillis() - start;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "SYNCHRONOUS");
        response.put("totalTimeMs", totalTime);
        response.put("results", List.of(emailResult, auditResult, syncResult));
        response.put("explanation", "Tasks ran sequentially on the same thread. Total time = sum of all tasks.");
        return response;
    }

    @GetMapping("/async")
    public Map<String, Object> runAsync() throws Exception {
        long start = System.currentTimeMillis();
        String userName = "DemoUser";

        CompletableFuture<String> emailFuture = asyncUserService.sendWelcomeEmailAsync(userName);
        CompletableFuture<String> auditFuture = asyncUserService.writeAuditLogAsync(userName);
        CompletableFuture<String> syncFuture = asyncUserService.syncToExternalSystemAsync(userName);

        CompletableFuture.allOf(emailFuture, auditFuture, syncFuture).join();

        long totalTime = System.currentTimeMillis() - start;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "ASYNCHRONOUS");
        response.put("totalTimeMs", totalTime);
        response.put("results", List.of(emailFuture.get(), auditFuture.get(), syncFuture.get()));
        response.put("explanation", "Tasks ran in parallel on separate threads. Total time = longest task.");
        return response;
    }

}
