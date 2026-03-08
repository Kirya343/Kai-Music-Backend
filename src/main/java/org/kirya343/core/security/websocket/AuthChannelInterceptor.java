package org.kirya343.core.security.websocket;

import java.util.Collection;
import java.util.Map;

import org.kirya343.core.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@Profile("production")
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthChannelInterceptor.class);

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

            if (sessionAttributes == null) throw new MessagingException("Missing session attributes");
            
            String token = (String) sessionAttributes.get("accessToken");

            if (token == null) throw new MessagingException("Missing access token");

            try {
                String openId = jwtService.validateAndGetOpenId(token);

                if (openId == null) throw new MessagingException("Invalid or expired token");

                Collection<GrantedAuthority> authorities = jwtService.getAuthorities(token);

                Authentication authentication =
                    new UsernamePasswordAuthenticationToken(openId, null, authorities);

                accessor.setUser(authentication);

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                logger.debug("STOMP user authenticated: {}", context);

            } catch (Exception e) {
                logger.error("STOMP token validation failed", e);
                throw new MessagingException(e.getMessage(), e);
            }
        }

        return message;
    }
}