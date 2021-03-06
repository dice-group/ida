package org.dice.ida;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	/**
	 * A websocket configurer class!
	 * manages topic subscription and app url
	 * prefix for end points
	 */

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// This end point will be used if sockets are not available
		registry.addEndpoint("/ida-fb-ws").setAllowedOrigins("*").withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic");
		// Setting prefix for all socket end points
		config.setApplicationDestinationPrefixes("/ida");
	}

}
