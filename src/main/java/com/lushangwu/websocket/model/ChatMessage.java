package com.lushangwu.websocket.model;

import java.time.LocalDateTime;

/**
 * 聊天消息模型类
 * 
 * 用于表示在WebSocket通信中传递的消息对象
 */
public class ChatMessage {

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        CHAT,       // 普通聊天消息
        JOIN,       // 用户加入消息
        LEAVE       // 用户离开消息
    }

    private MessageType type;    // 消息类型
    private String content;      // 消息内容
    private String sender;       // 发送者
    private LocalDateTime time;  // 发送时间

    /**
     * 默认构造函数
     */
    public ChatMessage() {
        this.time = LocalDateTime.now();
    }

    /**
     * 带参数的构造函数
     * 
     * @param type 消息类型
     * @param content 消息内容
     * @param sender 发送者
     */
    public ChatMessage(MessageType type, String content, String sender) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.time = LocalDateTime.now();
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "type=" + type +
                ", content='" + content + '\'' +
                ", sender='" + sender + '\'' +
                ", time=" + time +
                '}';
    }
} 