package com.shike.delivery.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.delivery.domain.FoodOrder;
import com.shike.delivery.security.AuthPrincipal;
import com.shike.delivery.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = UriComponentsBuilder.fromUri(session.getUri())
            .build()
            .getQueryParams()
            .getFirst("token");
        try {
            AuthPrincipal principal = jwtService.parseToken(token);
            sessions.put(session.getId(), new SessionInfo(session, principal));
        } catch (RuntimeException exception) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid token"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    public void notifyOrderUpdate(FoodOrder order, String message) {
        Map<String, Object> payload = Map.of(
            "type", "ORDER_UPDATED",
            "orderId", order.getId(),
            "orderNo", order.getOrderNo(),
            "status", order.getStatus().name(),
            "message", message
        );
        try {
            String json = objectMapper.writeValueAsString(payload);
            sessions.values().forEach(info -> {
                if (canReceive(info.principal(), order)) {
                    send(info.session(), json);
                }
            });
        } catch (IOException exception) {
            log.warn("Failed to serialize order notification", exception);
        }
    }

    private boolean canReceive(AuthPrincipal principal, FoodOrder order) {
        return switch (principal.role()) {
            case "USER" -> principal.id().equals(order.getAccountId());
            case "MERCHANT" -> principal.merchantId() != null
                && principal.merchantId().equals(order.getMerchantId());
            case "RIDER" -> principal.id().equals(order.getRiderId());
            case "ADMIN" -> true;
            default -> false;
        };
    }

    private void send(WebSocketSession session, String json) {
        if (!session.isOpen()) {
            sessions.remove(session.getId());
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException exception) {
            log.debug("Failed to push websocket message", exception);
        }
    }

    private record SessionInfo(WebSocketSession session, AuthPrincipal principal) {
    }
}
