package io.linkinben.springbootsecurityjwt.configs;

import java.net.Socket;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.linkinben.springbootsecurityjwt.services.message.entities.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class SocketConfig implements WebSocketMessageBrokerConfigurer {
    Logger logger = LoggerFactory.getLogger(Socket.class);

    @Autowired
    protected Messages trackingVariable;

    @Autowired
    private SimpMessageSendingOperations simpMessageSendingOperations;
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        WebSocketMessageBrokerConfigurer.super.configureMessageBroker(registry);
        // .setTaskScheduler(heartBeatScheduler())
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        WebSocketMessageBrokerConfigurer.super.registerStompEndpoints(registry);
        registry.addEndpoint("/ws").setAllowedOrigins("http://localhost:4200").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                logger.info("Inbound interceptor: " + accessor);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    accessor.setUser(new Principal() {
                        @Override
                        public String getName() {
                            return accessor.getNativeHeader("username").get(0);
                        }
                    });
                    logger.info("Native header connect: " + accessor.getMessageHeaders());
                    String uniquePublicRoom = accessor.getNativeHeader("uniqueRoomId").get(0);
                    if (trackingVariable.getPublicSessionId().equals("NO_PUBLIC_ROOM")) {
                        logger.info("New unique Room: " + uniquePublicRoom);
                        trackingVariable.setPublicSessionId(uniquePublicRoom);
                    } else {
                        logger.info("Available unique Room: " + trackingVariable.getPublicSessionId());
                    }
                    accessor.setHeader("uniqueRoomId", trackingVariable.getPublicSessionId());
                }

                if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    if(!Objects.isNull(accessor.getNativeHeader("uniqueRoomId"))){
                        String uniqueRoomId = accessor.getNativeHeader("uniqueRoomId").get(0);
                        String username = accessor.getUser().getName();
                        logger.info("Disconnect room id: " + uniqueRoomId);
                        if (trackingVariable.getTrackingVariables().containsKey("online_users")) {
                            trackingVariable.getTrackingVariables().get("online_users").remove(username);
                        }
                        Messages chatMessages = new Messages();
                        chatMessages.setMessageType(Messages.MessageType.LEAVE);
                        chatMessages.setSender(username);
                        chatMessages.setContent(accessor.getUser().getName() + " has left the chat!");
                        chatMessages.setTrackingVariables(trackingVariable.getTrackingVariables());
                        simpMessageSendingOperations.convertAndSend("/topic/public/" + uniqueRoomId, chatMessages);
                    }
                }


                return message;
            }
        });
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
                logger.info("Outbound interceptor: " + headerAccessor);

                if (!Objects.isNull(headerAccessor)) {
                    GenericMessage messageConnectHeaders = (GenericMessage) headerAccessor.getMessageHeaders().get("simpConnectMessage");

                    if (!Objects.isNull(messageConnectHeaders)) {
                        logger.info("Outbound interceptor message header: " + messageConnectHeaders);
                        String commandType = messageConnectHeaders.getHeaders().get("stompCommand").toString();
                        String uniqueRoomId = messageConnectHeaders.getHeaders().get("uniqueRoomId").toString();

                        if (!Objects.isNull(uniqueRoomId) && StompCommand.CONNECT.toString().equals(commandType)) {
                            final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
                            accessor.setSessionId(headerAccessor.getSessionId());
                            logger.info("Outbound connect unique room id: " + uniqueRoomId);
                            final MultiValueMap<String, String> nativeHeaders = (MultiValueMap<String, String>) headerAccessor.getHeader(StompHeaderAccessor.NATIVE_HEADERS);
                            accessor.addNativeHeaders(nativeHeaders);
                            accessor.addNativeHeader("uniqueRoomId", uniqueRoomId);
                            accessor.setHeader("uniqueRoomId", uniqueRoomId);
                            accessor.setUser(headerAccessor.getUser());
                            final Message<?> newMessage = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
                            return newMessage;
                        }
                    }
                }
                
                return message;
            }
        });
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {

    }

    //    @Bean
//    public TaskScheduler heartBeatScheduler() {
//    	ThreadPoolTaskScheduler periodicalTask = new ThreadPoolTaskScheduler();
//        return periodicalTask;
//    }


}
