package vn.edu.fpt.myfschool.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void addSession(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void removeSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session); //xoa session cua phien do
            if (userSessions.isEmpty())
                sessions.remove(userId); //neu phien do khong co session thi xoa luong trong map di
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

    public int getOpenSessionCount(Long userId) { //hàm xem có bao nhiêu session websocket được mở
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) return 0;
        userSessions.removeIf(session -> !session.isOpen());
        return userSessions.size();
    }

    public boolean isOnline(Long userId) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        return userSessions != null && !userSessions.isEmpty(); //chỉ cần có phiên là online
    }
}
