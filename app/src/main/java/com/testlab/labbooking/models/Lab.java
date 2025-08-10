package com.testlab.labbooking.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Lab {
    private String id;
    private String name;
    private String description;
    private int capacity;
    private List<String> resources;
    private String location;
    private boolean isActive;
    private String imageUrl;
    private double hourlyRate;
    private String contactEmail;
    private String contactPhone;
    private String rules;
    private String equipment;

    // Metadata fields
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;
    private String createdBy;
    private String updatedBy;

    // Availability fields
    private List<String> availableDays; // Monday, Tuesday, etc.
    private String openTime; // Format: HH:MM (24-hour)
    private String closeTime; // Format: HH:MM (24-hour)
    private int maxBookingHours; // Maximum hours per booking
    private int advanceBookingDays; // How many days in advance can be booked
    private int minBookingMinutes; // Minimum booking duration in minutes
    private boolean requiresApproval; // Whether bookings need admin approval

    // New fields for better management
    private List<String> allowedUserTypes; // "student", "faculty", "admin"
    private String category; // "Computer Lab", "Research Lab", "Teaching Lab"
    private int priority; // For ordering labs (1 = highest priority)
    private boolean maintenanceMode; // Temporarily disable bookings
    private String maintenanceMessage; // Message to show when in maintenance

    // Statistics fields (calculated, not stored)
    @Exclude
    private int totalBookings;
    @Exclude
    private int pendingBookings;
    @Exclude
    private int todayBookings;
    @Exclude
    private double utilizationRate;

    public Lab() {
        // Empty constructor for Firestore
        this.resources = new ArrayList<>();
        this.availableDays = new ArrayList<>();
        this.allowedUserTypes = new ArrayList<>();
        this.isActive = true;
        this.capacity = 1;
        this.maxBookingHours = 4;
        this.advanceBookingDays = 30;
        this.minBookingMinutes = 30;
        this.hourlyRate = 0.0;
        this.requiresApproval = true;
        this.priority = 1;
        this.maintenanceMode = false;

        // Default available days (Monday to Friday)
        this.availableDays.add("Monday");
        this.availableDays.add("Tuesday");
        this.availableDays.add("Wednesday");
        this.availableDays.add("Thursday");
        this.availableDays.add("Friday");

        // Default allowed user types
        this.allowedUserTypes.add("student");
        this.allowedUserTypes.add("faculty");
    }

    public Lab(String name, String description, int capacity, String location) {
        this();
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.location = location;
    }

    public Lab(String id, String name, String description, int capacity,
               List<String> resources, String location) {
        this(name, description, capacity, location);
        this.id = id;
        this.resources = resources != null ? new ArrayList<>(resources) : new ArrayList<>();
    }

    // Basic getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public List<String> getResources() {
        return resources != null ? resources : new ArrayList<>();
    }

    public void setResources(List<String> resources) {
        this.resources = resources != null ? new ArrayList<>(resources) : new ArrayList<>();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @PropertyName("isActive")
    public boolean isActive() {
        return isActive && !maintenanceMode;
    }

    @PropertyName("isActive")
    public void setActive(boolean active) {
        isActive = active;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = Math.max(0, hourlyRate);
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    // Metadata getters and setters
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    // Availability getters and setters
    public List<String> getAvailableDays() {
        return availableDays != null ? availableDays : new ArrayList<>();
    }

    public void setAvailableDays(List<String> availableDays) {
        this.availableDays = availableDays != null ? new ArrayList<>(availableDays) : new ArrayList<>();
    }

    public String getOpenTime() {
        return openTime != null ? openTime : "08:00";
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public String getCloseTime() {
        return closeTime != null ? closeTime : "18:00";
    }

    public void setCloseTime(String closeTime) {
        this.closeTime = closeTime;
    }

    public int getMaxBookingHours() {
        return maxBookingHours;
    }

    public void setMaxBookingHours(int maxBookingHours) {
        this.maxBookingHours = Math.max(1, maxBookingHours);
    }

    public int getAdvanceBookingDays() {
        return advanceBookingDays;
    }

    public void setAdvanceBookingDays(int advanceBookingDays) {
        this.advanceBookingDays = Math.max(1, advanceBookingDays);
    }

    public int getMinBookingMinutes() {
        return minBookingMinutes;
    }

    public void setMinBookingMinutes(int minBookingMinutes) {
        this.minBookingMinutes = Math.max(15, minBookingMinutes);
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    // New field getters and setters
    public List<String> getAllowedUserTypes() {
        return allowedUserTypes != null ? allowedUserTypes : new ArrayList<>();
    }

    public void setAllowedUserTypes(List<String> allowedUserTypes) {
        this.allowedUserTypes = allowedUserTypes != null ? new ArrayList<>(allowedUserTypes) : new ArrayList<>();
    }

    public String getCategory() {
        return category != null ? category : "General Lab";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = Math.max(1, priority);
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public String getMaintenanceMessage() {
        return maintenanceMessage != null ? maintenanceMessage : "Lab is under maintenance";
    }

    public void setMaintenanceMessage(String maintenanceMessage) {
        this.maintenanceMessage = maintenanceMessage;
    }

    // Statistics getters and setters (excluded from Firestore)
    @Exclude
    public int getTotalBookings() {
        return totalBookings;
    }

    @Exclude
    public void setTotalBookings(int totalBookings) {
        this.totalBookings = totalBookings;
    }

    @Exclude
    public int getPendingBookings() {
        return pendingBookings;
    }

    @Exclude
    public void setPendingBookings(int pendingBookings) {
        this.pendingBookings = pendingBookings;
    }

    @Exclude
    public int getTodayBookings() {
        return todayBookings;
    }

    @Exclude
    public void setTodayBookings(int todayBookings) {
        this.todayBookings = todayBookings;
    }

    @Exclude
    public double getUtilizationRate() {
        return utilizationRate;
    }

    @Exclude
    public void setUtilizationRate(double utilizationRate) {
        this.utilizationRate = Math.max(0, Math.min(100, utilizationRate));
    }

    // Utility methods
    public void addResource(String resource) {
        if (resource != null && !resource.trim().isEmpty()) {
            if (this.resources == null) {
                this.resources = new ArrayList<>();
            }
            String trimmedResource = resource.trim();
            if (!this.resources.contains(trimmedResource)) {
                this.resources.add(trimmedResource);
            }
        }
    }

    public void removeResource(String resource) {
        if (this.resources != null && resource != null) {
            this.resources.remove(resource.trim());
        }
    }

    @Exclude
    public boolean hasResource(String resource) {
        return this.resources != null && resource != null &&
                this.resources.contains(resource.trim());
    }

    @Exclude
    public String getResourcesAsString() {
        if (resources == null || resources.isEmpty()) {
            return "No resources listed";
        }
        return String.join(", ", resources);
    }

    @Exclude
    public boolean isAvailableOnDay(String day) {
        return availableDays != null && day != null &&
                availableDays.contains(day.trim());
    }

    @Exclude
    public boolean isUserTypeAllowed(String userType) {
        return allowedUserTypes != null && userType != null &&
                allowedUserTypes.contains(userType.toLowerCase());
    }

    @Exclude
    public String getAvailabilitySummary() {
        if (availableDays == null || availableDays.isEmpty()) {
            return "Availability not specified";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.join(", ", availableDays));
        summary.append(" (").append(getOpenTime()).append(" - ").append(getCloseTime()).append(")");

        return summary.toString();
    }

    @Exclude
    public String getStatusText() {
        if (maintenanceMode) {
            return "Under Maintenance";
        }
        if (!isActive) {
            return "Inactive";
        }
        return "Available";
    }

    @Exclude
    public boolean isBookingAllowed() {
        return isActive && !maintenanceMode;
    }

    @Exclude
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
                location != null && !location.trim().isEmpty() &&
                capacity > 0 &&
                openTime != null && !openTime.trim().isEmpty() &&
                closeTime != null && !closeTime.trim().isEmpty();
    }

    @Exclude
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add("Lab name is required");
        }

        if (location == null || location.trim().isEmpty()) {
            errors.add("Location is required");
        }

        if (capacity <= 0) {
            errors.add("Capacity must be greater than 0");
        }

        if (maxBookingHours <= 0) {
            errors.add("Maximum booking hours must be greater than 0");
        }

        if (advanceBookingDays <= 0) {
            errors.add("Advance booking days must be greater than 0");
        }

        if (openTime == null || openTime.trim().isEmpty()) {
            errors.add("Opening time is required");
        }

        if (closeTime == null || closeTime.trim().isEmpty()) {
            errors.add("Closing time is required");
        }

        if (availableDays == null || availableDays.isEmpty()) {
            errors.add("At least one available day must be selected");
        }

        return errors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lab lab = (Lab) o;
        return Objects.equals(id, lab.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Lab{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", capacity=" + capacity +
                ", isActive=" + isActive +
                ", maintenanceMode=" + maintenanceMode +
                ", category='" + category + '\'' +
                '}';
    }

    @Exclude
    public Lab copy() {
        Lab copy = new Lab();
        copy.setId(this.id);
        copy.setName(this.name);
        copy.setDescription(this.description);
        copy.setCapacity(this.capacity);
        copy.setResources(new ArrayList<>(this.getResources()));
        copy.setLocation(this.location);
        copy.setActive(this.isActive);
        copy.setImageUrl(this.imageUrl);
        copy.setHourlyRate(this.hourlyRate);
        copy.setContactEmail(this.contactEmail);
        copy.setContactPhone(this.contactPhone);
        copy.setRules(this.rules);
        copy.setEquipment(this.equipment);
        copy.setCreatedAt(this.createdAt);
        copy.setUpdatedAt(this.updatedAt);
        copy.setCreatedBy(this.createdBy);
        copy.setUpdatedBy(this.updatedBy);
        copy.setAvailableDays(new ArrayList<>(this.getAvailableDays()));
        copy.setOpenTime(this.openTime);
        copy.setCloseTime(this.closeTime);
        copy.setMaxBookingHours(this.maxBookingHours);
        copy.setAdvanceBookingDays(this.advanceBookingDays);
        copy.setMinBookingMinutes(this.minBookingMinutes);
        copy.setRequiresApproval(this.requiresApproval);
        copy.setAllowedUserTypes(new ArrayList<>(this.getAllowedUserTypes()));
        copy.setCategory(this.category);
        copy.setPriority(this.priority);
        copy.setMaintenanceMode(this.maintenanceMode);
        copy.setMaintenanceMessage(this.maintenanceMessage);
        return copy;
    }
}