package io.linkinben.springbootsecurityjwt.controllers.message.entities;

public class Message {

    private MessageType messageType;
    private String sender;
    private String content;
    private String uniqueSessionId;

    public enum MessageType {
        CHAT, JOIN, LEAVE, REQUEST_DATA
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Message() {
    }


    public Message(MessageType messageType, String sender, String content) {
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


}
