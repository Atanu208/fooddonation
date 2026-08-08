package com.app.fooddonation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

/**
 * Requires an authenticated session for every STOMP message. Only logged-in
 * users can receive live notifications; anonymous WebSocket connections are
 * rejected at the message layer.
 */
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                .simpMessageDestMatchers("/app/**").authenticated()
                .anyMessage().authenticated();
    }

    /**
     * The app is served from the same origin as the SockJS connection, so the
     * extra STOMP CSRF handshake is not required.
     */
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}
