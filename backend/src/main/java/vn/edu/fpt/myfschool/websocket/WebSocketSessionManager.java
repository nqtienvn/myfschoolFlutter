package vn.edu.fpt.myfschool.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void addSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            userSessions = ConcurrentHashMap.newKeySet(); //thread safe: cac luong dung cham nhau van an toan
            sessions.put(userId, userSessions);
        }
        userSessions.add(session);
    }

    public void removeSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session); //xoa session cua phien do
            if (userSessions.isEmpty()) sessions.remove(userId); //neu phien do khong co session thi xoa luong trong map di
        }
    }

    public void sendToUser(Long userId, String message) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message)); //send message cho tat ca session co trong set
                    } catch (IOException ignored) {
                        //bat loi o day la khong send duoc message
                    }
                }
            });
        }
    }

    public boolean isOnline(Long userId) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        return userSessions != null && !userSessions.isEmpty(); //chỉ cần có phiên là online
    }
}
