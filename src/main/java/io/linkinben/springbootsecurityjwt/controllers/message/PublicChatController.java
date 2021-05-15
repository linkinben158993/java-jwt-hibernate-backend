package io.linkinben.springbootsecurityjwt.controllers.message;

import io.linkinben.springbootsecurityjwt.controllers.message.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.*;

@Controller
public class PublicChatController {
    private Logger logger = LoggerFactory.getLogger(PublicChatController.class);

    private Map<String, List<Message>> publicMessages = new HashMap<String, List<Message>>();

    @MessageMapping("/public/message/{publicSession}")
    @SendTo("/topic/public")
    public Message sendMessage(Message publicMessage, @DestinationVariable String publicSession) {
        if (publicMessage.getMessageType().equals(Message.MessageType.CHAT)) {
            logger.info("From " + publicMessage.getSender() + " message type: " + publicMessage.getMessageType() + " content: " + publicMessage.getContent() + " to public/message -> /topic/public");

            if (publicMessages.containsKey(publicSession)) {
                publicMessages.get(publicSession).add(publicMessage);
            } else {
                List<Message> messages = new ArrayList<>();
                messages.add(publicMessage);
                publicMessages.put(publicSession, messages);
            }

            logger.info("Public message count: " + publicMessages.get(publicSession).size());

            return new Message(Message.MessageType.CHAT, publicMessage.getSender(), publicMessage.getContent());
        }
        return null;
    }

    @MessageMapping("/public/messages/{publicSession}")
    @SendTo("/topic/public")
    public List<Message> requestAllMessages(Message publicMessage, @DestinationVariable String publicSession) {
        if (publicMessage.getMessageType().equals(Message.MessageType.REQUEST_DATA)) {
            logger.info("From " + publicMessage.getSender() + " message type: " + publicMessage.getMessageType() + " content: " + publicMessage.getContent() + " to public/message -> /topic/public");
            if (publicMessages.containsKey(publicSession)) {
                logger.info("Public message count: " + publicMessages.get(publicSession).size());
                return this.publicMessages.get(publicSession);
            }
        }
        return null;
    }

    @MessageMapping("/public/messages/{publicSession}/clean")
    public void cleanSessionMessages(Message publicMessage, @DestinationVariable String publicSession) {
        logger.info("Clean: " + publicMessage.getSender());
    }
}
