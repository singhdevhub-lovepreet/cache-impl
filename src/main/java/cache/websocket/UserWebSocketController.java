package cache.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class UserWebSocketController {

    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public Map<String, String> handlePing(Map<String, String> message) {
        return Map.of(
                "type", "PONG",
                "echo", message.getOrDefault("message", ""),
                "serverTime", LocalDateTime.now().toString(),
                "thread", Thread.currentThread().getName()
        );
    }
}
