package com.lushangwu.websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类
 * 
 * 该配置类用于注册WebSocket端点，启用WebSocket通信功能
 * WebSocket是一种在单个TCP连接上进行全双工通信的协议
 * 
 * @Configuration：表示这是一个配置类
 */
@Configuration
public class WebSocketConfig {
    
    /**
     * 注册WebSocket端点导出器
     * 
     * 该Bean会自动注册使用@ServerEndpoint注解声明的WebSocket端点
     * 
     * @return ServerEndpointExporter实例
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
} 