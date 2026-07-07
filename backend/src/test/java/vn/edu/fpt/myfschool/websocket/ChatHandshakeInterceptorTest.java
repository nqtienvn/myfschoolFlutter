package vn.edu.fpt.myfschool.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import vn.edu.fpt.myfschool.security.JwtTokenProvider;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatHandshakeInterceptorTest {

    private JwtTokenProvider jwtTokenProvider;
    private ChatHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        interceptor = new ChatHandshakeInterceptor(jwtTokenProvider);
    }

    @Test
    void beforeHandshake_withoutToken_returnsFalseAndSets401() throws Exception {
        // Arrange
        ServerHttpRequest request = mockRequest("ws://localhost:8080/chat"); // no token
        ServerHttpResponse response = mockResponse();
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = Collections.emptyMap();

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertThat(result).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void beforeHandshake_withInvalidToken_returnsFalseAndSets401() throws Exception {
        // Arrange
        ServerHttpRequest request = mockRequest("ws://localhost:8080/chat?token=invalid_token_123");
        ServerHttpResponse response = mockResponse();
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = Collections.emptyMap();

        when(jwtTokenProvider.validateToken("invalid_token_123")).thenReturn(false);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertThat(result).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(jwtTokenProvider).validateToken("invalid_token_123");
    }

    @Test
    void beforeHandshake_withExpiredToken_returnsFalseAndSets401() throws Exception {
        // Arrange
        ServerHttpRequest request = mockRequest("ws://localhost:8080/chat?token=expired_jwt_token");
        ServerHttpResponse response = mockResponse();
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = Collections.emptyMap();

        when(jwtTokenProvider.validateToken("expired_jwt_token")).thenReturn(false);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertThat(result).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshake_withValidToken_returnsTrueAndSetsUserId() throws Exception {
        // Arrange
        ServerHttpRequest request = mockRequest("ws://localhost:8080/chat?token=valid_jwt_token_abc");
        ServerHttpResponse response = mockResponse();
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new java.util.HashMap<>();

        when(jwtTokenProvider.validateToken("valid_jwt_token_abc")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid_jwt_token_abc")).thenReturn(7L);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertThat(result).isTrue();
        verify(jwtTokenProvider).validateToken("valid_jwt_token_abc");
        verify(jwtTokenProvider).getUserIdFromToken("valid_jwt_token_abc");
        assertThat(attributes.get("userId")).isEqualTo(7L);
    }

    @Test
    void beforeHandshake_withTokenInAuthorizationHeader_works() throws Exception {
        // Arrange
        ServerHttpRequest request = mockRequestWithHeader("ws://localhost:8080/chat", "Bearer header_token_xyz");
        ServerHttpResponse response = mockResponse();
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new java.util.HashMap<>();

        when(jwtTokenProvider.validateToken("header_token_xyz")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("header_token_xyz")).thenReturn(10L);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertThat(result).isTrue();
        assertThat(attributes.get("userId")).isEqualTo(10L);
    }

    private ServerHttpRequest mockRequest(String uri) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(uri));
        when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
        return request;
    }

    private ServerHttpRequest mockRequestWithHeader(String uri, String authHeader) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(uri));
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", authHeader);
        when(request.getHeaders()).thenReturn(headers);
        return request;
    }

    private ServerHttpResponse mockResponse() {
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        doNothing().when(response).setStatusCode(any());
        return response;
    }
}