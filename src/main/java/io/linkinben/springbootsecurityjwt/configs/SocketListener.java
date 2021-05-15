package io.linkinben.springbootsecurityjwt.configs;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import io.linkinben.springbootsecurityjwt.controllers.message.entities.Message;

@Component
public class SocketListener {
    private Logger logger = LoggerFactory.getLogger(SocketListener.class);

    protected String publicChannel = "";
    protected Map<String, List<String>> tracking_variable = new HashMap<>();

    @Autowired
    private SimpMessageSendingOperations simpMessageSendingOperations;

    @EventListener
    public void handleWebSocketConnectionListener(SessionConnectedEvent event) {
        logger.info("Received new connection!");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        logger.info("User headers: " + headerAccessor.getUser().getName());
        String username = headerAccessor.getUser().getName();

        GenericMessage messageHeaders = (GenericMessage) headerAccessor.getMessageHeaders().get("simpConnectMessage");
        logger.info("Room id headers: " + messageHeaders.getHeaders().get("uniqueRoomId"));
        String publicRoom = messageHeaders.getHeaders().get("uniqueRoomId").toString();

        if (!publicChannel.equals("")) {
            logger.info("No need to create new public channel");
        } else {
            logger.info("Create new public channel!");
            publicChannel = publicRoom;
        }

        if (tracking_variable.containsKey("online_users")) {
            tracking_variable.get("online_users").add(username);
        } else {
            List<String> listOnlineUsers = new ArrayList<String>();
            listOnlineUsers.add(username);
            tracking_variable.put("online_users", listOnlineUsers);
        }

        logger.info("User connecting: " + username + ".");
        logger.info("Online user: " + tracking_variable.get("online_users").size() + ".");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        logger.info("User disconnected!");

        String username = headerAccessor.getUser().getName();
        if (username != null) {
            logger.info("User Disconnected : " + username);

            if (tracking_variable.containsKey("online_users")) {
                tracking_variable.get("online_users").remove(username);
                Message chatMessage = new Message();
                chatMessage.setMessageType(Message.MessageType.LEAVE);
                chatMessage.setSender(username);
                chatMessage.setContent(username + " has left the chat!");

                simpMessageSendingOperations.convertAndSend("/topic/public", chatMessage);
            }

            logger.info("User disconnected: " + username + ".");
            logger.info("Online user: " + tracking_variable.get("online_users").size() + ".");
        }
    }
}
