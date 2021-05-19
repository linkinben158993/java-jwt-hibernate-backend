package io.linkinben.springbootsecurityjwt.configs;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import io.linkinben.springbootsecurityjwt.services.message.entities.Messages;

@Component
public class SocketListener {
    private Logger logger = LoggerFactory.getLogger(SocketListener.class);

    @Autowired
    protected Messages trackingVariable;

    @EventListener
    public void handleWebSocketConnectionListener(SessionConnectedEvent event) {
        logger.info("Received new connection!");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        logger.info("User headers: " + headerAccessor.getUser().getName());
        String username = headerAccessor.getUser().getName();

        String publicRoom = headerAccessor.getMessageHeaders().get("uniqueRoomId").toString();
        logger.info("Public room id: " + publicRoom);

        if (trackingVariable.getTrackingVariables().containsKey("online_users")) {
            trackingVariable.getTrackingVariables().get("online_users").add(username);
        } else {
            List<String> listOnlineUsers = new ArrayList<String>();
            listOnlineUsers.add(username);
            trackingVariable.getTrackingVariables().put("online_users", listOnlineUsers);
        }

        logger.info("User connecting: " + username + ".");
        logger.info("Online user: " + trackingVariable.getTrackingVariables().get("online_users").size() + ".");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = headerAccessor.getUser().getName();
        logger.info("Disconnect handler: " + headerAccessor.getMessageHeaders());
        if (username != null) {
            logger.info("User disconnected: " + username + ".");
            logger.info("Online user: " + trackingVariable.getTrackingVariables().get("online_users").size() + ".");
        }
    }
}
