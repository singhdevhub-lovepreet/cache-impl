package cache.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseEmitterService.register();
    }

    @GetMapping("/connections")
    public Map<String, Integer> getConnections() {
        return Map.of("activeConnections", sseEmitterService.getConnectionCount());
    }
}
