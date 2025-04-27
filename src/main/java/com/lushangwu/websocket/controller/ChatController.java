package com.lushangwu.websocket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * 聊天控制器
 * 
 * 处理聊天页面的请求路由
 * Controller注解：表示这是一个控制器类
 * RequestMapping注解：指定该控制器的基础URL路径
 */
@Controller
@RequestMapping("/")
public class ChatController {

    /**
     * 聊天室首页，提供登录界面
     * 
     * @return 首页视图
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 进入聊天室
     * 
     * @param userId 用户ID
     * @return 聊天室页面
     */
    @GetMapping("/chat/{userId}")
    public String chat(@PathVariable("userId") String userId) {
        // 重定向到静态资源
        return "redirect:/chat.html?userId=" + userId;
    }
} 