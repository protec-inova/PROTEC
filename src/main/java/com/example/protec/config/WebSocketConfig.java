package com.example.protec.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.example.protec.maquinas.MaquinaGateway;

@Configuration
@EnableWebSocket
@EnableScheduling
public class WebSocketConfig implements WebSocketConfigurer {

    private final MaquinaGateway maquinaGateway;

    public WebSocketConfig(MaquinaGateway maquinaGateway) {
        this.maquinaGateway = maquinaGateway;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(maquinaGateway, "/ws/pi").setAllowedOrigins("*");
    }
}