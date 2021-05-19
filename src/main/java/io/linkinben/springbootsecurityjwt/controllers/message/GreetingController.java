package io.linkinben.springbootsecurityjwt.controllers.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import io.linkinben.springbootsecurityjwt.services.message.entities.Messages;

@Controller
public class GreetingController {
    private Logger logger = LoggerFactory.getLogger(GreetingController.class);

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Messages greeting(Messages message) throws Exception {
        logger.info(message.getSender() + " at hello -> topic/greetings! " + message.getContent());
        return new Messages(Messages.MessageType.JOIN, "Public Server", "Hello, " + message.getSender() + " !");
    }

    @MessageMapping("/join")
    @SendTo("/topic/public")
    public Messages join(Messages message) throws Exception {
        logger.info(message.getSender() + " at join -> topic/public! " + message.getContent());
        if (message.getMessageType().equals(Messages.MessageType.JOIN)) {
            return new Messages(Messages.MessageType.JOIN, "Public Server", message.getContent());
        }
        return null;
    }
}
