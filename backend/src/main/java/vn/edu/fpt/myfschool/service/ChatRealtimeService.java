package vn.edu.fpt.myfschool.service;

import org.springframework.web.socket.WebSocketSession;

public interface ChatRealtimeService {
    void handle(Long userId, WebSocketSession session, String payload);
    void handleConnected(Long userId);
    void handleDisconnected(Long userId);
}
