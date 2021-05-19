package io.linkinben.springbootsecurityjwt.services.message.entities;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Messages {

    private MessageType messageType;
    private String sender;
    private String content;

    private String publicSessionId = "NO_PUBLIC_ROOM";
    private Map<String, List<String>> trackingVariables = new HashMap<>();
    private Map<String, List<Messages>> trackingMessages = new HashMap<>();

    public enum MessageType {
        CHAT, JOIN, LEAVE, REQUEST_DATA
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Messages() {

    }


    public Messages(MessageType messageType, String sender, String content) {
        this.messageType = messageType;
        this.sender = sender;
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPublicSessionId() {
        return publicSessionId;
    }

    public void setPublicSessionId(String publicSessionId) {
        this.publicSessionId = publicSessionId;
    }

    public Map<String, List<String>> getTrackingVariables() {
        return trackingVariables;
    }
    
    public void setTrackingVariables(Map<String, List<String>> trackingVariable) {
        this.trackingVariables = trackingVariable;
    }

    public Map<String, List<Messages>> getTrackingMessages() {
        return trackingMessages;
    }

    public void setTrackingMessages(Map<String, List<Messages>> trackingMessages) {
        this.trackingMessages = trackingMessages;
    }
    
}
