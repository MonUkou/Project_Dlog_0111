package com.example.myapplication.service;

import com.example.myapplication.model.Notification;
import java.util.List;

public interface INotificationService {

    /**
     * type chỉ chấp nhận: LIKE, COMMENT, FOLLOW.
     */
    Notification createNotification(
            String receiverId,
            String senderId,
            String type,
            String postId
    );

    List<Notification> getNotifications(String userId);

    void markAsRead(String notificationId);

    Integer getUnreadCount(String userId);
}
