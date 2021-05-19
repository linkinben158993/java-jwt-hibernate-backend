package io.linkinben.springbootsecurityjwt.controllers.message;

import io.linkinben.springbootsecurityjwt.services.message.entities.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.*;

@Controller
public class PublicChatController {
    private Logger logger = LoggerFactory.getLogger(PublicChatController.class);

    @Autowired
    protected Messages trackingVariable;

    @MessageMapping("/join/{publicRoomId}")
    @SendTo("/topic/public/{publicRoomId}")
    public Messages join(Messages messages, @DestinationVariable String publicRoomId) throws Exception {
        logger.info(messages.getSender() + " at join -> topic/public/" + publicRoomId + ": " + messages.getContent());
        if (messages.getMessageType().equals(Messages.MessageType.JOIN)) {
            Messages responseMessages = new Messages(Messages.MessageType.JOIN, "Public Server", messages.getContent());
            responseMessages.setTrackingVariables(trackingVariable.getTrackingVariables());
            return responseMessages;
        }
        return null;
    }
    
    @MessageMapping("/public/message/{publicRoomId}")
    @SendTo("/topic/public/{publicRoomId}")
    public Messages sendMessage(Messages publicMessages, @DestinationVariable String publicRoomId) {
        logger.info("Public Room Id: " + publicRoomId);
        String publicSession = trackingVariable.getPublicSessionId();
        logger.info("Public Session: " + publicSession);

        if (publicMessages.getMessageType().equals(Messages.MessageType.CHAT)) {
            logger.info("From " + publicMessages.getSender() + " message type: " + publicMessages.getMessageType() + " content: " + publicMessages.getContent() + " to public/message -> /topic/public");

            Map<String, List<Messages>> trackMessages = trackingVariable.getTrackingMessages();

            if (trackMessages.containsKey(publicSession)) {
                trackMessages.get(publicSession).add(publicMessages);
            } else {
                List<Messages> messages = new ArrayList<>();
                messages.add(publicMessages);
                trackMessages.put(publicSession, messages);
            }

            logger.info("Public message count: " + trackMessages.get(publicSession).size());

            return new Messages(Messages.MessageType.CHAT, publicMessages.getSender(), publicMessages.getContent());
        }
        return null;
    }

    @MessageMapping("/public/messages/{publicRoomId}")
    @SendTo("/topic/public/{publicRoomId}")
    public Messages requestAllMessages(Messages publicMessages, @DestinationVariable String publicRoomId) {
        logger.info("Public Room Id: " + publicRoomId);
        String publicSession = trackingVariable.getPublicSessionId();
        logger.info("Public Session: " + publicSession);

        if (publicMessages.getMessageType().equals(Messages.MessageType.REQUEST_DATA)) {
            logger.info("From " + publicMessages.getSender() + " message type: " + publicMessages.getMessageType() + " content: " + publicMessages.getContent() + " to public/message -> /topic/public");

            Map<String, List<Messages>> trackMessages = trackingVariable.getTrackingMessages();
            Messages requestMessages = new Messages(Messages.MessageType.REQUEST_DATA, publicMessages.getSender(), publicMessages.getContent());
            requestMessages.setPublicSessionId(trackingVariable.getPublicSessionId());
            requestMessages.setTrackingMessages(trackMessages);

            if (trackMessages.containsKey(publicSession)) {
                logger.info("Public message count: " + trackMessages.get(publicSession).size());
                return requestMessages;
            }
        }
        return null;
    }

    @MessageMapping("/public/messages/{publicRoomId}/clean")
    public void cleanSessionMessages(Messages publicMessages, @DestinationVariable String publicRoomId) {
        logger.info("Cleaning session: " + publicRoomId);
        if (trackingVariable.getTrackingVariables().get("online_users").size() == 0) {
            logger.info("Cleaning this session: " + trackingVariable.getPublicSessionId());
            trackingVariable.getTrackingMessages().remove(trackingVariable.getPublicSessionId());
            trackingVariable.setPublicSessionId("NO_PUBLIC_ROOM");
        }
    }
}
