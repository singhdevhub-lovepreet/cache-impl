package cache.async;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncUserService { //

    // ---- Async versions (run on async-demo- thread pool) ----

    @Async("asyncExecutor")
    public CompletableFuture<String> sendWelcomeEmailAsync(String userName) {
        return CompletableFuture.completedFuture(simulateEmail(userName));
    }

    @Async("asyncExecutor")
    public CompletableFuture<String> writeAuditLogAsync(String userName) {
        return CompletableFuture.completedFuture(simulateAudit(userName));
    }

    @Async("asyncExecutor")
    public CompletableFuture<String> syncToExternalSystemAsync(String userName) {
        return CompletableFuture.completedFuture(simulateExternalSync(userName));
    }

    // ---- Synchronous versions (run on calling thread) ----

    public String sendWelcomeEmailSync(String userName) {
        return simulateEmail(userName);
    }

    public String writeAuditLogSync(String userName) {
        return simulateAudit(userName);
    }

    public String syncToExternalSystemSync(String userName) {
        return simulateExternalSync(userName);
    }

    // ---- Simulated work ----

    private String simulateEmail(String userName) {
        long start = System.currentTimeMillis();
        sleep(2000);
        long duration = System.currentTimeMillis() - start;
        return String.format("[%s] Email sent to %s (%dms)",
                Thread.currentThread().getName(), userName, duration);
    }

    private String simulateAudit(String userName) {
        long start = System.currentTimeMillis();
        sleep(1500);
        long duration = System.currentTimeMillis() - start;
        return String.format("[%s] Audit log written for %s (%dms)",
                Thread.currentThread().getName(), userName, duration);
    }

    private String simulateExternalSync(String userName) {
        long start = System.currentTimeMillis();
        sleep(3000);
        long duration = System.currentTimeMillis() - start;
        return String.format("[%s] External sync completed for %s (%dms)",
                Thread.currentThread().getName(), userName, duration);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
