package com.testlab.labbooking.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.firestore.Exclude;
// import com.google.firebase.firestore.PropertyName; // No longer needed for 'status'
import com.google.firebase.firestore.ServerTimestamp;
import com.testlab.labbooking.utils.DateTimeUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Booking implements Parcelable, Serializable {
    private String id;
    private String labId;
    private String userId;
    private String labName; // Denormalized for easier queries
    private String userName; // Denormalized for easier queries
    private String userEmail; // For notifications
    private String userPhone; // For SMS notifications
    private String date; // Format: YYYY-MM-DD
    private String startTime; // Format: HH:MM (24-hour)
    private String endTime; // Format: HH:MM (24-hour)
    private BookingStatus status; // Use enum instead of string
    private String purpose;
    private String additionalNotes; // User notes/requirements
    private String adminNotes; // Admin feedback/notes
    private String cancellationReason;
    private int numberOfParticipants; // How many people will use the lab
    private List<String> requiredResources; // Specific resources needed

    // Approval/Review information
    private String reviewedBy; // Admin who reviewed the booking
    private Date reviewedAt; // When it was reviewed
    private int priority; // Booking priority (1 = highest)

    // Reminder and notification tracking
    private boolean reminderSent;
    private Date reminderSentAt;
    private boolean followUpSent;
    private Date followUpSentAt;

    // Cost and billing
    private double totalCost; // Calculated based on duration and hourly rate
    private boolean isPaid;
    private String paymentReference;

    // Recurrence support
    private boolean isRecurring;
    private String recurrencePattern; // "weekly", "daily", etc.
    private Date recurrenceEndDate;
    private String parentBookingId; // For recurring bookings

    // Check-in/Check-out
    private boolean checkedIn;
    private Date checkInTime;
    private boolean checkedOut;
    private Date checkOutTime;
    private String actualUsageDuration; // Actual time spent

    // Metadata
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;

    public Booking() {
        // Empty constructor for Firestore
        this.status = BookingStatus.PENDING;
        this.numberOfParticipants = 1;
        this.requiredResources = new ArrayList<>();
        this.priority = 1;
        this.reminderSent = false;
        this.followUpSent = false;
        this.isPaid = false;
        this.isRecurring = false;
        this.checkedIn = false;
        this.checkedOut = false;
    }

    public Booking(String labId, String userId, String labName, String userName,
                   String date, String startTime, String endTime, String purpose) {
        this();
        this.labId = labId;
        this.userId = userId;
        this.labName = labName;
        this.userName = userName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
    }

    protected Booking(Parcel in) {
        id = in.readString();
        labId = in.readString();
        userId = in.readString();
        labName = in.readString();
        userName = in.readString();
        userEmail = in.readString();
        userPhone = in.readString();
        date = in.readString();
        startTime = in.readString();
        endTime = in.readString();
        status = BookingStatus.valueOf(in.readString());
        purpose = in.readString();
        additionalNotes = in.readString();
        adminNotes = in.readString();
        cancellationReason = in.readString();
        numberOfParticipants = in.readInt();
        requiredResources = in.createStringArrayList();
        reviewedBy = in.readString();
        reviewedAt = (Date) in.readSerializable();
        priority = in.readInt();
        reminderSent = in.readByte() != 0;
        reminderSentAt = (Date) in.readSerializable();
        followUpSent = in.readByte() != 0;
        followUpSentAt = (Date) in.readSerializable();
        totalCost = in.readDouble();
        isPaid = in.readByte() != 0;
        paymentReference = in.readString();
        isRecurring = in.readByte() != 0;
        recurrencePattern = in.readString();
        recurrenceEndDate = (Date) in.readSerializable();
        parentBookingId = in.readString();
        checkedIn = in.readByte() != 0;
        checkInTime = (Date) in.readSerializable();
        checkedOut = in.readByte() != 0;
        checkOutTime = (Date) in.readSerializable();
        actualUsageDuration = in.readString();
        createdAt = (Date) in.readSerializable();
        updatedAt = (Date) in.readSerializable();
    }

    public static final Creator<Booking> CREATOR = new Creator<Booking>() {
        @Override
        public Booking createFromParcel(Parcel in) {
            return new Booking(in);
        }

        @Override
        public Booking[] newArray(int size) {
            return new Booking[size];
        }
    };

    // Basic getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabId() {
        return labId;
    }

    public void setLabId(String labId) {
        this.labId = labId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLabName() {
        return labName;
    }

    public void setLabName(String labName) {
        this.labName = labName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
        updateTotalCost();
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
        updateTotalCost();
    }

    public BookingStatus getStatus() {
        return status != null ? status : BookingStatus.PENDING;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public int getNumberOfParticipants() {
        return numberOfParticipants;
    }

    public void setNumberOfParticipants(int numberOfParticipants) {
        this.numberOfParticipants = Math.max(1, numberOfParticipants);
    }

    public List<String> getRequiredResources() {
        return requiredResources != null ? requiredResources : new ArrayList<>();
    }

    public void setRequiredResources(List<String> requiredResources) {
        this.requiredResources = requiredResources != null ? new ArrayList<>(requiredResources) : new ArrayList<>();
    }

    // Review information
    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = Math.max(1, Math.min(5, priority)); // 1-5 scale
    }

    // Notification tracking
    public boolean isReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }

    public Date getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(Date reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }

    public boolean isFollowUpSent() {
        return followUpSent;
    }

    public void setFollowUpSent(boolean followUpSent) {
        this.followUpSent = followUpSent;
    }

    public Date getFollowUpSentAt() {
        return followUpSentAt;
    }

    public void setFollowUpSentAt(Date followUpSentAt) {
        this.followUpSentAt = followUpSentAt;
    }

    // Cost and billing
    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = Math.max(0, totalCost);
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    // Recurrence
    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public String getRecurrencePattern() {
        return recurrencePattern;
    }

    public void setRecurrencePattern(String recurrencePattern) {
        this.recurrencePattern = recurrencePattern;
    }

    public Date getRecurrenceEndDate() {
        return recurrenceEndDate;
    }

    public void setRecurrenceEndDate(Date recurrenceEndDate) {
        this.recurrenceEndDate = recurrenceEndDate;
    }

    public String getParentBookingId() {
        return parentBookingId;
    }

    public void setParentBookingId(String parentBookingId) {
        this.parentBookingId = parentBookingId;
    }

    // Check-in/Check-out
    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public Date getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Date checkInTime) {
        this.checkInTime = checkInTime;
    }

    public boolean isCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

    public Date getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Date checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getActualUsageDuration() {
        return actualUsageDuration;
    }

    public void setActualUsageDuration(String actualUsageDuration) {
        this.actualUsageDuration = actualUsageDuration;
    }

    // Metadata
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    @Exclude
    public int getDurationMinutes() {
        if (startTime != null && endTime != null) {
            return DateTimeUtils.getTimeDifferenceInMinutes(startTime, endTime);
        }
        return 0;
    }

    @Exclude
    public double getDurationHours() {
        return getDurationMinutes() / 60.0;
    }

    @Exclude
    public String getFormattedDuration() {
        int minutes = getDurationMinutes();
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (hours > 0 && remainingMinutes > 0) {
            return hours + "h " + remainingMinutes + "m";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return remainingMinutes + "m";
        }
    }

    @Exclude
    public String getTimeSlot() {
        return startTime + " - " + endTime;
    }

    @Exclude
    public String getDateTimeDisplay() {
        return date + " at " + getTimeSlot();
    }

    @Exclude
    public boolean isPending() {
        return status == BookingStatus.PENDING;
    }

    @Exclude
    public boolean isApproved() {
        return status == BookingStatus.APPROVED;
    }

    @Exclude
    public boolean isRejected() {
        return status == BookingStatus.REJECTED;
    }

    @Exclude
    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    @Exclude
    public boolean isCompleted() {
        return status == BookingStatus.COMPLETED;
    }

    @Exclude
    public boolean isActive() {
        return status == BookingStatus.PENDING || status == BookingStatus.APPROVED;
    }

    @Exclude
    public boolean canBeCancelled() {
        return isActive() && !DateTimeUtils.isDateTimePast(date, startTime);
    }

    @Exclude
    public boolean canBeModified() {
        return isPending() && !DateTimeUtils.isDateTimePast(date, startTime);
    }

    @Exclude
    public boolean requiresPayment() {
        return totalCost > 0 && !isPaid;
    }

    @Exclude
    public boolean isToday() {
        return DateTimeUtils.isToday(date);
    }

    @Exclude
    public boolean isUpcoming() {
        return DateTimeUtils.isFutureDate(date);
    }

    @Exclude
    public boolean isOverdue() {
        return isApproved() && DateTimeUtils.isDateTimePast(date, endTime) && !checkedOut;
    }

    @Exclude
    public void addRequiredResource(String resource) {
        if (resource != null && !resource.trim().isEmpty()) {
            if (this.requiredResources == null) {
                this.requiredResources = new ArrayList<>();
            }
            String trimmed = resource.trim();
            if (!this.requiredResources.contains(trimmed)) {
                this.requiredResources.add(trimmed);
            }
        }
    }

    @Exclude
    public void removeRequiredResource(String resource) {
        if (this.requiredResources != null && resource != null) {
            this.requiredResources.remove(resource.trim());
        }
    }

    @Exclude
    public void updateTotalCost() {
        // This would be called when duration or hourly rate changes
        // Implementation would calculate cost based on lab's hourly rate
    }

    @Exclude
    public void approve(String adminId, String notes) {
        this.status = BookingStatus.APPROVED;
        this.reviewedBy = adminId;
        this.reviewedAt = new Date();
        this.adminNotes = notes;
    }

    @Exclude
    public void reject(String adminId, String reason) {
        this.status = BookingStatus.REJECTED;
        this.reviewedBy = adminId;
        this.reviewedAt = new Date();
        this.adminNotes = reason;
    }

    @Exclude
    public void cancel(String reason) {
        this.status = BookingStatus.CANCELLED;
        this.cancellationReason = reason;
    }

    @Exclude
    public void checkIn() {
        this.checkedIn = true;
        this.checkInTime = new Date();
    }

    @Exclude
    public void checkOut() {
        this.checkedOut = true;
        this.checkOutTime = new Date();

        if (checkInTime != null) {
            long durationMs = checkOutTime.getTime() - checkInTime.getTime();
            int actualMinutes = (int) (durationMs / (1000 * 60));
            this.actualUsageDuration = DateTimeUtils.formatDuration(actualMinutes);
        }

        // Auto-complete if checked out
        if (status == BookingStatus.APPROVED) {
            this.status = BookingStatus.COMPLETED;
        }
    }

    @Exclude
    public boolean isValid() {
        return labId != null && !labId.trim().isEmpty() &&
                userId != null && !userId.trim().isEmpty() &&
                date != null && !date.trim().isEmpty() &&
                startTime != null && !startTime.trim().isEmpty() &&
                endTime != null && !endTime.trim().isEmpty() &&
                purpose != null && !purpose.trim().isEmpty() &&
                numberOfParticipants > 0;
    }

    @Exclude
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();

        if (labId == null || labId.trim().isEmpty()) {
            errors.add("Lab ID is required");
        }

        if (userId == null || userId.trim().isEmpty()) {
            errors.add("User ID is required");
        }

        if (date == null || date.trim().isEmpty()) {
            errors.add("Date is required");
        }

        if (startTime == null || startTime.trim().isEmpty()) {
            errors.add("Start time is required");
        }

        if (endTime == null || endTime.trim().isEmpty()) {
            errors.add("End time is required");
        }

        if (purpose == null || purpose.trim().isEmpty()) {
            errors.add("Purpose is required");
        }

        if (numberOfParticipants <= 0) {
            errors.add("Number of participants must be greater than 0");
        }

        // Validate time logic
        if (startTime != null && endTime != null) {
            if (DateTimeUtils.isTimeAfter(startTime, endTime)) {
                errors.add("Start time must be before end time");
            }

            int duration = getDurationMinutes();
            if (duration < 15) {
                errors.add("Booking duration must be at least 15 minutes");
            }
        }

        // Validate date is not in the past
        if (date != null && DateTimeUtils.isPastDate(date)) {
            errors.add("Cannot book for past dates");
        }

        return errors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(id, booking.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id='" + id + '\'' +
                ", labName='" + labName + '\'' +
                ", userName='" + userName + '\'' +
                ", date='" + date + '\'' +
                ", timeSlot='" + getTimeSlot() + '\'' +
                ", status=" + status +
                ", numberOfParticipants=" + numberOfParticipants +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(labId);
        dest.writeString(userId);
        dest.writeString(labName);
        dest.writeString(userName);
        dest.writeString(userEmail);
        dest.writeString(userPhone);
        dest.writeString(date);
        dest.writeString(startTime);
        dest.writeString(endTime);
        dest.writeString(status.name()); // Still use status.name() for Parcelable
        dest.writeString(purpose);
        dest.writeString(additionalNotes);
        dest.writeString(adminNotes);
        dest.writeString(cancellationReason);
        dest.writeInt(numberOfParticipants);
        dest.writeStringList(requiredResources);
        dest.writeString(reviewedBy);
        dest.writeSerializable(reviewedAt);
        dest.writeInt(priority);
        dest.writeByte((byte) (reminderSent ? 1 : 0));
        dest.writeSerializable(reminderSentAt);
        dest.writeByte((byte) (followUpSent ? 1 : 0));
        dest.writeSerializable(followUpSentAt);
        dest.writeDouble(totalCost);
        dest.writeByte((byte) (isPaid ? 1 : 0));
        dest.writeString(paymentReference);
        dest.writeByte((byte) (isRecurring ? 1 : 0));
        dest.writeString(recurrencePattern);
        dest.writeSerializable(recurrenceEndDate);
        dest.writeString(parentBookingId);
        dest.writeByte((byte) (checkedIn ? 1 : 0));
        dest.writeSerializable(checkInTime);
        dest.writeByte((byte) (checkedOut ? 1 : 0));
        dest.writeSerializable(checkOutTime);
        dest.writeString(actualUsageDuration);
        dest.writeSerializable(createdAt);
        dest.writeSerializable(updatedAt);
    }
}
