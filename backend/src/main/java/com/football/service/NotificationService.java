package com.football.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.football.entity.Notification;

import java.util.List;

public interface NotificationService extends IService<Notification> {

    List<Notification> getByUserId(Long userId);

    Notification createNotification(Long userId, String title, String content, String type);

    void markAsRead(Long id);

    void markAllAsRead(Long userId);

    Long getUnreadCount(Long userId);
}
