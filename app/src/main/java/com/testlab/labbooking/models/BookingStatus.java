package com.testlab.labbooking.models;

public enum BookingStatus {
    PENDING("pending", "Pending Review", "#FFA726", true),
    APPROVED("approved", "Approved", "#66BB6A", true),
    REJECTED("rejected", "Rejected", "#EF5350", false),
    CANCELLED("cancelled", "Cancelled", "#BDBDBD", false),
    COMPLETED("completed", "Completed", "#42A5F5", false),
    IN_PROGRESS("in_progress", "In Progress", "#26C6DA", true),
    OVERDUE("overdue", "Overdue", "#FF7043", true),
    NO_SHOW("no_show", "No Show", "#8D6E63", false);

    private final String value;
    private final String displayName;
    private final String colorCode;
    private final boolean isActive;

    BookingStatus(String value, String displayName, String colorCode, boolean isActive) {
        this.value = value;
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.isActive = isActive;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public boolean isActive() {
        return isActive;
    }

    /**
     * Get status from string value
     */
    public static BookingStatus fromString(String value) {
        if (value == null) return PENDING;

        for (BookingStatus status : BookingStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return PENDING; // Default fallback
    }

    /**
     * Check if status can be modified by user
     */
    public boolean canUserModify() {
        return this == PENDING;
    }

    /**
     * Check if status can be cancelled by user
     */
    public boolean canUserCancel() {
        return this == PENDING || this == APPROVED;
    }

    /**
     * Check if status can be approved by admin
     */
    public boolean canAdminApprove() {
        return this == PENDING;
    }

    /**
     * Check if status can be rejected by admin
     */
    public boolean canAdminReject() {
        return this == PENDING;
    }

    /**
     * Check if booking requires check-in
     */
    public boolean requiresCheckIn() {
        return this == APPROVED;
    }

    /**
     * Check if booking can be checked out
     */
    public boolean canCheckOut() {
        return this == APPROVED || this == IN_PROGRESS;
    }

    /**
     * Get next possible statuses for transitions
     */
    public BookingStatus[] getPossibleTransitions() {
        switch (this) {
            case PENDING:
                return new BookingStatus[]{APPROVED, REJECTED, CANCELLED};
            case APPROVED:
                return new BookingStatus[]{IN_PROGRESS, CANCELLED, NO_SHOW, OVERDUE};
            case IN_PROGRESS:
                return new BookingStatus[]{COMPLETED, CANCELLED};
            case OVERDUE:
                return new BookingStatus[]{COMPLETED, NO_SHOW, CANCELLED};
            default:
                return new BookingStatus[]{};
        }
    }

    /**
     * Check if this status indicates the booking is actionable
     */
    public boolean isActionable() {
        return this == PENDING || this == APPROVED || this == IN_PROGRESS || this == OVERDUE;
    }

    /**
     * Get appropriate icon name for status
     */
    public String getIconName() {
        switch (this) {
            case PENDING:
                return "clock";
            case APPROVED:
                return "check-circle";
            case REJECTED:
                return "x-circle";
            case CANCELLED:
                return "slash";
            case COMPLETED:
                return "check-square";
            case IN_PROGRESS:
                return "play-circle";
            case OVERDUE:
                return "alert-triangle";
            case NO_SHOW:
                return "user-x";
            default:
                return "help-circle";
        }
    }

    /**
     * Get status description for notifications
     */
    public String getNotificationMessage(String labName) {
        switch (this) {
            case PENDING:
                return "Your booking for " + labName + " is pending review";
            case APPROVED:
                return "Your booking for " + labName + " has been approved";
            case REJECTED:
                return "Your booking for " + labName + " has been rejected";
            case CANCELLED:
                return "Your booking for " + labName + " has been cancelled";
            case COMPLETED:
                return "Your booking for " + labName + " has been completed";
            case IN_PROGRESS:
                return "Your booking for " + labName + " is now in progress";
            case OVERDUE:
                return "Your booking for " + labName + " is overdue";
            case NO_SHOW:
                return "You missed your booking for " + labName;
            default:
                return "Your booking for " + labName + " status has been updated";
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}