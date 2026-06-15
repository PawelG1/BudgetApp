package pk.gp.pasir_galusza_pawel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import pk.gp.pasir_galusza_pawel.security.JwtUtil;
import pk.gp.pasir_galusza_pawel.service.GroupNotificationWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtUtil jwtUtil;
    private final GroupNotificationWebSocketHandler handler;

    public WebSocketConfig(JwtUtil jwtUtil, GroupNotificationWebSocketHandler handler) {
        this.jwtUtil = jwtUtil;
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/group-notifications")
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil))
                .setAllowedOriginPatterns("*");
    }
}
