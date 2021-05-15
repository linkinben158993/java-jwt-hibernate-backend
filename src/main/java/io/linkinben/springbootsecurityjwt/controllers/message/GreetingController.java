package io.linkinben.springbootsecurityjwt.controllers.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import io.linkinben.springbootsecurityjwt.controllers.message.entities.Message;

@Controller
public class GreetingController {
    private Logger logger = LoggerFactory.getLogger(GreetingController.class);

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Message greeting(Message message) throws Exception {
        logger.info(message.getSender() + " at hello -> topic/greetings! " + message.getContent());
        return new Message(Message.MessageType.JOIN, "Public Server", "Hello, " + message.getSender() + " !");
    }

    @MessageMapping("/join")
    @SendTo("/topic/public")
    public Message join(Message message) throws Exception {
        logger.info(message.getSender() + " at join -> topic/public! " + message.getContent());
        if (message.getMessageType().equals(Message.MessageType.JOIN)) {
            return new Message(Message.MessageType.JOIN, "Public Server", message.getContent());
        }
        return null;
    }
}
