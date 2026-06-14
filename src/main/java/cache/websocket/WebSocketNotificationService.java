package cache.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
//    private final RestTemplate restTemplate;

    public void notifyUserCreated(Object userData) {
        messagingTemplate.convertAndSend("/topic/users", userData);
    }
}
 // eventPublisher