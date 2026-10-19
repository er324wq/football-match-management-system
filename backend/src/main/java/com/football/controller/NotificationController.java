package com.football.controller;

import com.football.entity.Notification;
import com.football.exception.ResourceNotFoundException;
import com.football.service.NotificationService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/user")
    public ResultVO<List<Notification>> getByUserId(@RequestParam Long userId,
                                                    @RequestAttribute("userId") Long authenticatedUserId,
                                                    @RequestAttribute("role") String role) {
        if (!isAdmin(role) && !authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("无权访问其他用户的通知");
        }
        return ResultVO.success(notificationService.getByUserId(userId));
    }

    @GetMapping("/unread-count")
    public ResultVO<Long> getUnreadCount(@RequestParam Long userId,
                                         @RequestAttribute("userId") Long authenticatedUserId,
                                         @RequestAttribute("role") String role) {
        if (!isAdmin(role) && !authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("无权访问其他用户的通知");
        }
        return ResultVO.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/read/{id}")
    public ResultVO<Void> markAsRead(@PathVariable Long id,
                                     @RequestAttribute("userId") Long authenticatedUserId,
                                     @RequestAttribute("role") String role) {
        Notification notification = notificationService.getById(id);
        if (notification == null) {
            throw new ResourceNotFoundException("通知不存在");
        }
        if (!isAdmin(role) && !authenticatedUserId.equals(notification.getUserId())) {
            throw new AccessDeniedException("无权修改其他用户的通知");
        }
        notificationService.markAsRead(id);
        return ResultVO.success();
    }

    @PutMapping("/read-all")
    public ResultVO<Void> markAllAsRead(@RequestParam Long userId,
                                        @RequestAttribute("userId") Long authenticatedUserId,
                                        @RequestAttribute("role") String role) {
        if (!isAdmin(role) && !authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("无权修改其他用户的通知");
        }
        notificationService.markAllAsRead(userId);
        return ResultVO.success();
    }

    private boolean isAdmin(String role) {
        return "admin".equals(role);
    }
}
