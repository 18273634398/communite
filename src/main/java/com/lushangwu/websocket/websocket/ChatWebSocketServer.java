package com.lushangwu.websocket.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket服务端点
 * 
 * ServerEndpoint注解：标注该类为WebSocket服务端点，指定连接路径
 * 客户端通过 ws://localhost:8080/chat/{userId} 连接到这个端点
 * {userId}是一个路径参数，用于标识连接的用户
 */
@Component
@ServerEndpoint("/chat/{userId}")
public class ChatWebSocketServer {

    /**
     * 日志对象
     */
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketServer.class);

    /**
     * 静态变量，用来记录当前在线连接数
     * AtomicInteger是一个线程安全的整型类，适合在高并发环境下使用
     */
    private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);
    
    /**
     * 用于存储当前WebSocket的会话对象，使用ConcurrentHashMap保证线程安全
     * key: userId, value: WebSocket会话
     */
    private static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 当WebSocket连接建立成功时调用
     * 
     * @param session WebSocket会话对象
     * @param userId 用户ID，从路径参数中获取
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        try {
            // URL解码，处理中文用户名
            String decodedUserId = java.net.URLDecoder.decode(userId, "UTF-8");
            
            // 添加用户会话
            SESSION_MAP.put(decodedUserId, session);
            // 在线人数加1
            int count = ONLINE_COUNT.incrementAndGet();
            log.info("有新连接加入，用户ID：{}，当前在线人数：{}", decodedUserId, count);
            
            // 延迟发送欢迎消息，等待连接完全建立
            new Thread(() -> {
                try {
                    // 等待连接稳定
                    Thread.sleep(500);
                    // 广播通知所有用户有新用户加入
                    String message = "欢迎用户" + decodedUserId + "加入聊天室！当前在线人数：" + count;
                    broadcastAll(message);
                } catch (InterruptedException e) {
                    log.error("延迟发送欢迎消息出错：", e);
                }
            }).start();
        } catch (Exception e) {
            log.error("处理WebSocket连接时出错：", e);
        }
    }

    /**
     * 当WebSocket连接关闭时调用
     * 
     * @param userId 用户ID
     */
    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        try {
            // URL解码，处理中文用户名
            String decodedUserId = java.net.URLDecoder.decode(userId, "UTF-8");
            
            // 移除用户会话
            SESSION_MAP.remove(decodedUserId);
            // 在线人数减1
            int count = ONLINE_COUNT.decrementAndGet();
            log.info("有连接关闭，用户ID：{}，当前在线人数：{}", decodedUserId, count);
            
            // 广播通知所有用户有用户离开
            String message = "用户" + decodedUserId + "离开聊天室！当前在线人数：" + count;
            broadcastAll(message);
        } catch (Exception e) {
            log.error("处理WebSocket关闭时出错：", e);
        }
    }

    /**
     * 当收到客户端消息时调用
     * 
     * @param message 客户端发送的消息
     * @param userId 发送消息的用户ID
     */
    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        try {
            // URL解码，处理中文用户名
            String decodedUserId = java.net.URLDecoder.decode(userId, "UTF-8");
            
            log.info("收到用户{}的消息：{}", decodedUserId, message);
            
            // 构造广播消息内容
            String broadcastMsg = "用户" + decodedUserId + "：" + message;
            // 广播消息给所有用户
            broadcastAll(broadcastMsg);
        } catch (Exception e) {
            log.error("处理WebSocket消息时出错：", e);
        }
    }

    /**
     * 当WebSocket通信发生错误时调用
     * 
     * @param session 会话对象
     * @param error 错误信息
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket发生错误：", error);
    }

    /**
     * 向特定用户发送消息
     * 
     * @param userId 用户ID
     * @param message 消息内容
     */
    public static void sendMessage(String userId, String message) {
        Session session = SESSION_MAP.get(userId);
        if (session != null && session.isOpen()) {
            try {
                // 使用单独的线程发送消息
                new Thread(() -> {
                    try {
                        session.getBasicRemote().sendText(message);
                        log.info("向用户{}发送消息：{}", userId, message);
                    } catch (IOException e) {
                        log.error("发送消息出错：{}", e.getMessage());
                    }
                }).start();
            } catch (Exception e) {
                log.error("发送消息出错：", e);
            }
        } else {
            log.warn("用户{}不在线", userId);
        }
    }

    /**
     * 广播消息给所有在线用户
     * 
     * @param message 广播消息内容
     */
    public static void broadcastAll(String message) {
        SESSION_MAP.forEach((userId, session) -> {
            try {
                if (session.isOpen()) {
                    // 使用单独的线程发送消息
                    new Thread(() -> {
                        try {
                            session.getBasicRemote().sendText(message);
                            log.info("向用户{}广播消息：{}", userId, message);
                        } catch (IOException e) {
                            log.error("向用户{}发送消息失败：{}", userId, e.getMessage());
                        }
                    }).start();
                }
            } catch (Exception e) {
                log.error("广播消息出错：", e);
            }
        });
    }
} 