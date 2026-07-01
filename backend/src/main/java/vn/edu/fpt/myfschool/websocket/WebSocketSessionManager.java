package vn.edu.fpt.myfschool.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> lastHeartbeats = new ConcurrentHashMap<>();

    public void addSession(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        heartbeat(userId);
    }

    public void removeSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) {
                sessions.remove(userId);
            }
        }
    }

    public void sendToUser(Long userId, String message) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) return;

        userSessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : userSessions) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException ex) {
                userSessions.remove(session);
            }
        }
        if (userSessions.isEmpty()) sessions.remove(userId);
    }

    public void sendToUsers(Collection<Long> userIds, String message) {
        userIds.forEach(userId -> sendToUser(userId, message));
    }

    public int getOpenSessionCount(Long userId) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) return 0;
        userSessions.removeIf(session -> !session.isOpen());
        if (userSessions.isEmpty()) sessions.remove(userId);
        return userSessions.size();
    }

    public boolean isOnline(Long userId) {
        return getOpenSessionCount(userId) > 0;
    }

    public void heartbeat(Long userId) {
        if (userId != null) {
            lastHeartbeats.put(userId, LocalDateTime.now());
        }
    }

    public LocalDateTime getLastHeartbeat(Long userId) {
        return lastHeartbeats.get(userId);
    }
}
