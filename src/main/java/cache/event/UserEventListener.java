package cache.event;

import cache.entity.User;
import cache.sse.SseEmitterService;
import cache.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final WebSocketNotificationService webSocketNotificationService;
    private final SseEmitterService sseEmitterService;

    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        User user = event.getUser();

        Map<String, Object> payload = Map.of(
                "event", "USER_CREATED",
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "timestamp", java.time.LocalDateTime.now().toString()
        );

        webSocketNotificationService.notifyUserCreated(payload);
        sseEmitterService.broadcast("user-created", payload);
    }
}
