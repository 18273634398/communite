# WebSocket聊天室项目详细说明文档

## 1. WebSocket简介

WebSocket是一种网络通信协议，提供全双工通信渠道，允许服务器和客户端之间进行实时双向数据传输。与HTTP不同，WebSocket建立连接后保持开放状态，从而可以持续进行双向通信。

WebSocket的主要特点：
- **持久连接**：一旦建立连接，可以保持打开状态直到客户端或服务器关闭它
- **实时双向通信**：服务器可以主动向客户端推送数据，不需要客户端发起请求
- **低延迟**：相比轮询和长轮询等技术，WebSocket具有更低的延迟和更高的效率
- **支持跨域**：WebSocket天然支持跨域通信

## 2. WebSocket工作原理

### 2.1 连接建立过程

1. **握手阶段**：
   - 客户端发送一个HTTP请求，请求头包含`Upgrade: websocket`和`Connection: Upgrade`等字段
   - 服务器回应一个HTTP响应，确认升级为WebSocket协议
   - 此时HTTP连接升级为WebSocket连接，后续通信使用WebSocket协议

2. **数据传输阶段**：
   - 连接建立后，客户端和服务器可以随时向对方发送消息
   - 消息可以是文本或二进制数据
   - 数据以帧(Frame)的形式传输

3. **连接关闭**：
   - 任何一方都可以发送关闭帧来终止连接
   - 对方接收到关闭帧后，应该回应一个关闭帧并关闭连接

### 2.2 WebSocket的URL格式

WebSocket使用不同于HTTP的协议标识符：
- `ws://` - 非加密WebSocket连接（类似于HTTP）
- `wss://` - 加密WebSocket连接（类似于HTTPS）

例如：`ws://localhost:8080/chat/user1`

## 3. 本项目WebSocket实现详解

### 3.1 项目架构

本项目使用Spring Boot实现WebSocket通信，主要组件包括：

1. **WebSocketConfig**：WebSocket配置类，注册WebSocket端点
2. **ChatWebSocketServer**：WebSocket服务端点，处理连接、消息和断开
3. **ChatController**：提供Web页面访问的控制器
4. **前端页面**：包括登录页(index.html)和聊天室页面(chat.html)

### 3.2 服务端实现

#### 3.2.1 WebSocket配置

首先，需要创建一个配置类来启用WebSocket功能：

```java
package com.lushangwu.websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig {
    
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
```

这个配置类通过`@Configuration`注解标记为Spring配置类，并注册了一个`ServerEndpointExporter` Bean。该Bean负责扫描并注册使用`@ServerEndpoint`注解的WebSocket端点。

#### 3.2.2 WebSocket服务端点

WebSocket服务端点是实现实时通信的核心组件。在本项目中，我们创建了`ChatWebSocketServer`类，使用`@ServerEndpoint`注解定义WebSocket端点的访问路径:

```java
@Component
@ServerEndpoint("/chat/{userId}")
public class ChatWebSocketServer {
    // WebSocket实现代码
}
```

端点路径`/chat/{userId}`中的`{userId}`是一个路径参数，用于标识连接的用户。

#### 3.2.3 会话管理

为了管理WebSocket连接，我们使用了两个静态变量：

```java
// 记录在线用户数量
private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);

// 存储所有活跃的WebSocket会话，key为用户ID，value为会话对象
private static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();
```

这里使用`AtomicInteger`和`ConcurrentHashMap`是为了保证在多线程环境下的线程安全。

#### 3.2.4 连接建立

当WebSocket连接建立时，使用`@OnOpen`注解的方法会被调用：

```java
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
```

这个方法完成了以下任务：
1. 解码用户ID（处理中文字符）
2. 将用户的WebSocket会话保存到SESSION_MAP
3. 增加在线人数计数
4. 通过新线程延迟发送欢迎消息给所有用户（避免连接状态冲突）

#### 3.2.5 消息处理

当服务器收到客户端消息时，使用`@OnMessage`注解的方法会被调用：

```java
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
```

这个方法的核心功能是：
1. 解码发送消息的用户ID
2. 记录日志
3. 构造包含用户ID的消息文本
4. 将消息广播给所有在线用户

#### 3.2.6 连接关闭

当WebSocket连接关闭时，使用`@OnClose`注解的方法会被调用：

```java
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
```

这个方法完成了以下任务：
1. 解码用户ID
2. 从SESSION_MAP中移除该用户的会话
3. 减少在线人数计数
4. 通知所有用户有用户离开聊天室

#### 3.2.7 错误处理

当WebSocket通信过程中发生错误时，使用`@OnError`注解的方法会被调用：

```java
@OnError
public void onError(Session session, Throwable error) {
    log.error("WebSocket发生错误：", error);
}
```

#### 3.2.8 消息广播机制

消息广播是聊天室的核心功能，通过遍历所有会话并发送消息实现：

```java
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
```

这个方法的关键点：
1. 遍历所有在线用户会话
2. 检查会话是否仍处于开放状态
3. 使用独立线程向每个会话发送消息
   - 使用独立线程的目的是防止WebSocket状态冲突
   - 在高并发情况下提高性能

### 3.3 前端实现

#### 3.3.1 登录页面

登录页面(index.html)主要功能是获取用户名并跳转到聊天室：

```javascript
// 登录表单提交处理
document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const userId = document.getElementById('userId').value.trim();
    
    if (userId) {
        // 使用encodeURI而不是encodeURIComponent，保留部分特殊字符
        // 跳转到聊天室页面，并传递用户ID
        const userIdParam = encodeURIComponent(userId);
        // 直接打开静态资源页面，避免通过控制器跳转
        window.location.href = '/chat.html?userId=' + userIdParam;
    }
});
```

这段代码的关键点是：
- 获取用户输入的用户名
- 使用encodeURIComponent正确编码用户名（特别是处理中文字符）
- 跳转到聊天室页面并传递用户名参数

#### 3.3.2 建立WebSocket连接

在聊天室页面(chat.html)中，首先需要建立WebSocket连接：

```javascript
// 连接WebSocket服务器
function connect() {
    try {
        // 获取用户ID并处理
        const userIdForWs = encodeURIComponent(userId);
        
        // 创建WebSocket连接
        // 添加调试信息
        const wsUrl = `ws://${window.location.host}/chat/${userIdForWs}`;
        document.getElementById('debugInfo').innerHTML += 
            `<div>尝试连接: ${wsUrl}</div>`;
        
        // 使用ws协议（如果是https则使用wss）
        // 连接到后端定义的/chat/{userId}端点
        socket = new WebSocket(wsUrl);
        
        // 设置WebSocket事件处理...
    } catch (e) {
        console.error('连接WebSocket出错:', e);
        addSystemMessage('连接聊天服务器失败: ' + e.message);
    }
}
```

这段代码的要点：
1. 对用户ID进行URL编码
2. 构建WebSocket连接URL（ws://host/chat/userId）
3. 创建WebSocket对象
4. 添加调试信息方便排查问题

#### 3.3.3 WebSocket事件处理

WebSocket API提供了四个事件用于处理不同的连接状态：

```javascript
// WebSocket连接打开时的处理
socket.onopen = function() {
    console.log('WebSocket连接已建立');
    isConnected = true;
    reconnectAttempts = 0; // 重置重连计数
    addSystemMessage('已连接到聊天服务器');
};

// 接收到WebSocket消息时的处理
socket.onmessage = function(event) {
    const message = event.data;
    console.log('收到消息:', message);
    
    // 解析消息中的在线人数
    const onlineCountMatch = message.match(/当前在线人数：(\d+)/);
    if (onlineCountMatch) {
        onlineCount.textContent = `在线人数: ${onlineCountMatch[1]}`;
    }
    
    // 显示接收到的消息
    if (message.startsWith('用户' + userId + '：')) {
        // 自己发送的消息
        const content = message.substring(('用户' + userId + '：').length);
        addMessage(userId, content, true);
    } else if (message.startsWith('用户')) {
        // 其他用户发送的消息
        const colonIndex = message.indexOf('：');
        if (colonIndex > -1) {
            const sender = message.substring(2, colonIndex);
            const content = message.substring(colonIndex + 1);
            addMessage(sender, content, false);
        } else {
            // 系统消息（用户加入/离开）
            addSystemMessage(message);
        }
    } else {
        // 其他系统消息
        addSystemMessage(message);
    }
};

// WebSocket连接关闭时的处理
socket.onclose = function(event) {
    console.log('WebSocket连接已关闭', event);
    isConnected = false;
    addSystemMessage('与聊天服务器断开连接');
    
    // 尝试重新连接
    if (reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++;
        const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000);
        addSystemMessage(`${delay/1000}秒后尝试重新连接... (${reconnectAttempts}/${maxReconnectAttempts})`);
        
        setTimeout(function() {
            connect();
        }, delay);
    } else {
        addSystemMessage('达到最大重连次数，请刷新页面重试');
    }
};

// WebSocket连接错误时的处理
socket.onerror = function(error) {
    console.error('WebSocket错误:', error);
    addSystemMessage('连接发生错误');
    document.getElementById('debugInfo').innerHTML += 
        '<div style="color:red">WebSocket连接错误</div>';
};
```

这些事件处理函数分别负责：
- `onopen`: 连接成功建立时的处理
- `onmessage`: 接收到服务器消息时的处理
  - 解析在线人数
  - 判断消息类型（自己发送的、他人发送的、系统消息）
  - 显示消息
- `onclose`: 连接关闭时的处理
  - 更新连接状态
  - 实现指数退避算法的重连机制
- `onerror`: 连接错误时的处理

#### 3.3.4 发送消息

用户在输入框中输入消息并点击发送按钮时，通过WebSocket发送消息：

```javascript
// 发送消息
function sendMessage() {
    const message = messageInput.value.trim();
    
    if (message && isConnected) {
        try {
            // 通过WebSocket发送消息
            socket.send(message);
            // 清空输入框
            messageInput.value = '';
        } catch (e) {
            console.error('发送消息出错:', e);
            addSystemMessage('发送消息失败: ' + e.message);
        }
    } else if (!isConnected) {
        addSystemMessage('未连接到服务器，无法发送消息');
    }
}

// 发送按钮点击事件
sendButton.addEventListener('click', sendMessage);

// 输入框按回车键发送消息
messageInput.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        sendMessage();
    }
});
```

这段代码的关键点：
1. 获取输入框中的消息内容
2. 检查消息是否为空及连接是否建立
3. 使用WebSocket的send方法发送消息
4. 清空输入框
5. 添加错误处理
6. 支持点击发送按钮或按回车键发送消息

## 4. 实时通信流程图

```
+----------------+      HTTP请求       +----------------+
|                |  ----------------> |                |
|   客户端浏览器   |                    |   Spring Boot   |
|   (前端页面)    |  <---------------- |   (后端服务器)   |
|                |      HTTP响应       |                |
+----------------+                    +----------------+
        |                                     |
        |    WebSocket握手(升级HTTP连接)        |
        | ---------------------------------> |
        |                                     |
        |     建立持久WebSocket连接            |
        | <--------------------------------- |
        |                                     |
        |                                     |
        |     消息1: 客户端 -> 服务器          |
        | ---------------------------------> |
        |                                     |
        |     消息2: 服务器 -> 所有客户端      |
        | <--------------------------------- |
        |                                     |
        |                                     |
        |     消息3: 客户端 -> 服务器          |
        | ---------------------------------> |
        |                                     |
        |     消息4: 服务器 -> 所有客户端      |
        | <--------------------------------- |
        |                                     |
```

## 5. 实现要点总结

1. **WebSocket配置**
   - 使用`@ServerEndpoint`注解定义WebSocket端点
   - 使用`ServerEndpointExporter` Bean注册WebSocket端点

2. **会话管理**
   - 使用`ConcurrentHashMap`存储用户会话
   - 使用`AtomicInteger`记录在线人数

3. **消息广播机制**
   - 遍历所有在线用户的会话
   - 使用独立线程发送消息，避免状态冲突

4. **URL编码处理**
   - 前端使用`encodeURIComponent`编码用户名
   - 后端使用`URLDecoder.decode`解码用户名

5. **异常处理**
   - 添加try-catch块捕获异常
   - 日志记录错误信息

6. **前端WebSocket生命周期**
   - 实现`onopen`、`onmessage`、`onclose`和`onerror`事件处理
   - 实现重连机制和指数退避算法

7. **UI交互**
   - 动态显示消息
   - 区分自己的消息、他人的消息和系统消息
   - 显示在线人数

## 6. 扩展学习

要深入学习WebSocket，可以参考以下资源：
- [WebSocket API 文档](https://developer.mozilla.org/zh-CN/docs/Web/API/WebSockets_API)
- [Spring WebSocket 文档](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [RFC 6455: WebSocket协议规范](https://tools.ietf.org/html/rfc6455)

## 7. 可能的改进方向

- 添加用户认证和授权
- 实现私聊功能
- 添加消息持久化（保存历史消息）
- 支持图片和文件传输
- 添加消息已读状态
- 实现消息加密
