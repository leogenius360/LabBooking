package com.testlab.labbooking.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.testlab.labbooking.utils.DateTimeUtils;

import java.util.Date;
import java.util.Objects;

public class Notification {
    private String id;
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private String relatedId; // ID of related booking, lab, etc.
    private String actionUrl; // Deep link or action URL
    private boolean read;
    private boolean dismissed;
    private int priority; // 1 = high, 2 = medium, 3 = low

    @ServerTimestamp
    private Date createdAt;
    private Date readAt;
    private Date dismissedAt;

    // Rich notification data
    private String imageUrl;
    private String iconName;
    private String actionText; // Text for action button
    private boolean persistent; // Should not auto-dismiss

    public Notification() {
        // Empty constructor for Firestore
        this.read = false;
        this.dismissed = false;
        this.priority = 2; // Default to medium priority
        this.persistent = false;
    }

    public Notification(String userId, String title, String message, NotificationType type) {
        this();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.priority = type.getDefaultPriority();
        this.iconName = type.getDefaultIcon();
    }

    public Notification(String userId, String title, String message, NotificationType type,
                        String relatedId) {
        this(userId, title, message, type);
        this.relatedId = relatedId;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type != null ? type : NotificationType.GENERAL;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    // String version for Firebase compatibility
    public String getTypeString() {
        return getType().getValue();
    }

    public void setTypeString(String type) {
        try {
            this.type = NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.type = NotificationType.GENERAL;
        }
    }

    public String getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
        if (read && readAt == null) {
            this.readAt = new Date();
        }
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
        if (dismissed && dismissedAt == null) {
            this.dismissedAt = new Date();
        }
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = Math.max(1, Math.min(3, priority));
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getReadAt() {
        return readAt;
    }

    public void setReadAt(Date readAt) {
        this.readAt = readAt;
    }

    public Date getDismissedAt() {
        return dismissedAt;
    }

    public void setDismissedAt(Date dismissedAt) {
        this.dismissedAt = dismissedAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getIconName() {
        return iconName != null ? iconName : getType().getDefaultIcon();
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getActionText() {
        return actionText != null ? actionText : getType().getDefaultActionText();
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    // ======================= UTILITY METHODS =======================

    @Exclude
    public String getTimeAgo() {
        if (createdAt != null) {
            long diffInMillis = System.currentTimeMillis() - createdAt.getTime();
            long minutes = diffInMillis / (1000 * 60);
            long hours = diffInMillis / (1000 * 60 * 60);
            long days = diffInMillis / (1000 * 60 * 60 * 24);

            if (minutes < 1) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " minute" + (minutes != 1 ? "s" : "") + " ago";
            } else if (hours < 24) {
                return hours + " hour" + (hours != 1 ? "s" : "") + " ago";
            } else if (days < 7) {
                return days + " day" + (days != 1 ? "s" : "") + " ago";
            } else {
                return DateTimeUtils.formatDateForDisplay(
                        DateTimeUtils.getCurrentDate()); // Fall back to formatted date
            }
        }
        return "Unknown";
    }

    @Exclude
    public String getPriorityText() {
        switch (priority) {
            case 1:
                return "High";
            case 2:
                return "Medium";
            case 3:
                return "Low";
            default:
                return "Medium";
        }
    }

    @Exclude
    public String getPriorityColor() {
        switch (priority) {
            case 1:
                return "#F44336"; // Red
            case 2:
                return "#FF9800"; // Orange
            case 3:
                return "#4CAF50"; // Green
            default:
                return "#FF9800";
        }
    }

    @Exclude
    public boolean isHighPriority() {
        return priority == 1;
    }

    @Exclude
    public boolean isVisible() {
        return !dismissed;
    }

    @Exclude
    public boolean requiresAction() {
        return actionUrl != null && !actionUrl.trim().isEmpty();
    }

    @Exclude
    public void markAsRead() {
        setRead(true);
    }

    @Exclude
    public void dismiss() {
        setDismissed(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", read=" + read +
                ", priority=" + priority +
                '}';
    }

    // ======================= NOTIFICATION TYPES ENUM =======================

    public enum NotificationType {
        BOOKING_APPROVED("booking_approved", "Booking Approved", "check-circle", "View Booking", 1),
        BOOKING_REJECTED("booking_rejected", "Booking Rejected", "x-circle", "View Details", 1),
        BOOKING_CANCELLED("booking_cancelled", "Booking Cancelled", "slash", "View Details", 2),
        BOOKING_REMINDER("booking_reminder", "Booking Reminder", "clock", "Check In", 1),
        BOOKING_OVERDUE("booking_overdue", "Booking Overdue", "alert-triangle", "Check Out", 1),
        BOOKING_COMPLETED("booking_completed", "Booking Completed", "check-square", "View Summary", 3),
        LAB_MAINTENANCE("lab_maintenance", "Lab Maintenance", "tool", "View Schedule", 2),
        LAB_UPDATED("lab_updated", "Lab Updated", "edit", "View Changes", 3),
        SYSTEM_UPDATE("system_update", "System Update", "download", "Learn More", 2),
        ADMIN_MESSAGE("admin_message", "Admin Message", "message-square", "View Message", 1),
        PAYMENT_REQUIRED("payment_required", "Payment Required", "credit-card", "Pay Now", 1),
        PAYMENT_CONFIRMED("payment_confirmed", "Payment Confirmed", "check", "View Receipt", 3),
        GENERAL("general", "Notification", "bell", "View", 2);

        private final String value;
        private final String displayName;
        private final String defaultIcon;
        private final String defaultActionText;
        private final int defaultPriority;

        NotificationType(String value, String displayName, String defaultIcon,
                         String defaultActionText, int defaultPriority) {
            this.value = value;
            this.displayName = displayName;
            this.defaultIcon = defaultIcon;
            this.defaultActionText = defaultActionText;
            this.defaultPriority = defaultPriority;
        }

        public String getValue() {
            return value;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDefaultIcon() {
            return defaultIcon;
        }

        public String getDefaultActionText() {
            return defaultActionText;
        }

        public int getDefaultPriority() {
            return defaultPriority;
        }

        public static NotificationType fromString(String value) {
            if (value == null) return GENERAL;

            for (NotificationType type : NotificationType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return GENERAL;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}