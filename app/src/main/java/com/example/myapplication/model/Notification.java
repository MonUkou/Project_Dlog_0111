package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;
import java.util.Map;

public class Notification extends BaseEntity {
    private String receiverId;
    private String senderId;
    private String senderUsername;
    private String type; // "LIKE", "COMMENT", "FOLLOW"
    private String postId;
    private boolean read;

    public Notification() {
        super();
        this.read = false;
    }

    public Notification(String id, String receiverId, String senderId, String senderUsername, String type, String postId) {
        super(id);
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.type = type;
        this.postId = postId;
        this.read = false;
    }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    @Override
    public boolean validate() {
        return receiverId != null && !receiverId.trim().isEmpty()
                && senderId != null && !senderId.trim().isEmpty()
                && type != null && !type.trim().isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = baseMap();
        map.put("receiverId", receiverId);
        map.put("senderId", senderId);
        map.put("senderUsername", senderUsername);
        map.put("type", type);
        map.put("postId", postId);
        map.put("read", read);
        return map;
    }

    public static Notification fromMap(Map<String, Object> map) {
        if (map == null) return null;
        Notification notification = new Notification();
        if (map.containsKey("id") && map.get("id") != null) notification.setId((String) map.get("id"));
        if (map.containsKey("receiverId") && map.get("receiverId") != null) notification.setReceiverId((String) map.get("receiverId"));
        if (map.containsKey("senderId") && map.get("senderId") != null) notification.setSenderId((String) map.get("senderId"));
        if (map.containsKey("senderUsername") && map.get("senderUsername") != null) notification.setSenderUsername((String) map.get("senderUsername"));
        if (map.containsKey("type") && map.get("type") != null) notification.setType((String) map.get("type"));
        if (map.containsKey("postId") && map.get("postId") != null) notification.setPostId((String) map.get("postId"));
        if (map.containsKey("read") && map.get("read") instanceof Boolean) notification.setRead((Boolean) map.get("read"));
        return notification;
    }
}
