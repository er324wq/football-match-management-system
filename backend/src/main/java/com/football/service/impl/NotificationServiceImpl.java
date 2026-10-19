package com.football.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.football.entity.Notification;
import com.football.mapper.NotificationMapper;
import com.football.service.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Override
    public List<Notification> getByUserId(Long userId) {
        return baseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Notification>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time")
        );
    }

    @Override
    public Notification createNotification(Long userId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setCreateTime(LocalDateTime.now());
        save(notification);
        return notification;
    }

    @Override
    public void markAsRead(Long id) {
        Notification notification = getById(id);
        if (notification != null) {
            notification.setIsRead(true);
            updateById(notification);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        baseMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Notification>()
                        .eq("user_id", userId)
                        .set("is_read", true)
        );
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return baseMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Notification>()
                        .eq("user_id", userId)
                        .eq("is_read", false)
        );
    }
}
