package com.testlab.labbooking.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class User implements Parcelable, Serializable {
    private String id;
    private String name;
    private String email;
    private String role; // "admin", "faculty", "student"
    private String phoneNumber;
    private String department;
    private String studentId; // For students
    private String employeeId; // For faculty/staff
    private boolean isActive;
    private boolean isVerified; // Email verification status

    // Profile information
    private String profileImageUrl;
    private String bio;
    private List<String> specializations; // For faculty
    private String yearOfStudy; // For students (e.g., "1st Year", "2nd Year")
    private String program; // For students (e.g., "Computer Science", "Engineering")

    // Preferences and settings
    private boolean emailNotifications;
    private boolean smsNotifications;
    private boolean pushNotifications;
    private String preferredLanguage;
    private String timezone;

    // Usage statistics (calculated, not stored)
    @Exclude
    private int totalBookings;
    @Exclude
    private int pendingBookings;
    @Exclude
    private int approvedBookings;
    @Exclude
    private int rejectedBookings;
    @Exclude
    private Date lastBookingDate;

    // Restrictions and limits
    private int maxSimultaneousBookings; // Max concurrent bookings allowed
    private int maxWeeklyHours; // Max hours per week
    private List<String> restrictedLabs; // Labs this user cannot book
    private boolean canBookWithoutApproval; // For trusted users

    // Metadata
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;
    private Date lastLoginAt;

    public User() {
        // Empty constructor for Firestore
        this.isActive = true;
        this.isVerified = false;
        this.emailNotifications = true;
        this.smsNotifications = false;
        this.pushNotifications = true;
        this.preferredLanguage = "en";
        this.timezone = "UTC";
        this.specializations = new ArrayList<>();
        this.restrictedLabs = new ArrayList<>();
        this.maxSimultaneousBookings = 3;
        this.maxWeeklyHours = 12;
        this.canBookWithoutApproval = false;
    }

    public User(String id, String name, String email, String role) {
        this();
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;

        // Set default limits based on role
        setDefaultLimitsByRole(role);
    }

    protected User(Parcel in) {
        id = in.readString();
        name = in.readString();
        email = in.readString();
        role = in.readString();
        phoneNumber = in.readString();
        department = in.readString();
        studentId = in.readString();
        employeeId = in.readString();
        isActive = in.readByte() != 0;
        isVerified = in.readByte() != 0;
        profileImageUrl = in.readString();
        bio = in.readString();
        specializations = in.createStringArrayList();
        yearOfStudy = in.readString();
        program = in.readString();
        emailNotifications = in.readByte() != 0;
        smsNotifications = in.readByte() != 0;
        pushNotifications = in.readByte() != 0;
        preferredLanguage = in.readString();
        timezone = in.readString();
        maxSimultaneousBookings = in.readInt();
        maxWeeklyHours = in.readInt();
        restrictedLabs = in.createStringArrayList();
        canBookWithoutApproval = in.readByte() != 0;
        createdAt = (Date) in.readSerializable();
        updatedAt = (Date) in.readSerializable();
        lastLoginAt = (Date) in.readSerializable();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
        setDefaultLimitsByRole(role);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    @PropertyName("isActive")
    public boolean isActive() {
        return isActive;
    }

    @PropertyName("isActive")
    public void setActive(boolean active) {
        isActive = active;
    }

    @PropertyName("isVerified")
    public boolean isVerified() {
        return isVerified;
    }

    @PropertyName("isVerified")
    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    // Profile getters and setters
    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public List<String> getSpecializations() {
        return specializations != null ? specializations : new ArrayList<>();
    }

    public void setSpecializations(List<String> specializations) {
        this.specializations = specializations != null ? new ArrayList<>(specializations) : new ArrayList<>();
    }

    public String getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(String yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    // Notification preferences
    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public boolean isSmsNotifications() {
        return smsNotifications;
    }

    public void setSmsNotifications(boolean smsNotifications) {
        this.smsNotifications = smsNotifications;
    }

    public boolean isPushNotifications() {
        return pushNotifications;
    }

    public void setPushNotifications(boolean pushNotifications) {
        this.pushNotifications = pushNotifications;
    }

    public String getPreferredLanguage() {
        return preferredLanguage != null ? preferredLanguage : "en";
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getTimezone() {
        return timezone != null ? timezone : "UTC";
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    // Restrictions and limits
    public int getMaxSimultaneousBookings() {
        return maxSimultaneousBookings;
    }

    public void setMaxSimultaneousBookings(int maxSimultaneousBookings) {
        this.maxSimultaneousBookings = Math.max(1, maxSimultaneousBookings);
    }

    public int getMaxWeeklyHours() {
        return maxWeeklyHours;
    }

    public void setMaxWeeklyHours(int maxWeeklyHours) {
        this.maxWeeklyHours = Math.max(1, maxWeeklyHours);
    }

    public List<String> getRestrictedLabs() {
        return restrictedLabs != null ? restrictedLabs : new ArrayList<>();
    }

    public void setRestrictedLabs(List<String> restrictedLabs) {
        this.restrictedLabs = restrictedLabs != null ? new ArrayList<>(restrictedLabs) : new ArrayList<>();
    }

    public boolean isCanBookWithoutApproval() {
        return canBookWithoutApproval;
    }

    public void setCanBookWithoutApproval(boolean canBookWithoutApproval) {
        this.canBookWithoutApproval = canBookWithoutApproval;
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

    public Date getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    // Statistics (excluded from Firestore)
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
    public int getApprovedBookings() {
        return approvedBookings;
    }

    @Exclude
    public void setApprovedBookings(int approvedBookings) {
        this.approvedBookings = approvedBookings;
    }

    @Exclude
    public int getRejectedBookings() {
        return rejectedBookings;
    }

    @Exclude
    public void setRejectedBookings(int rejectedBookings) {
        this.rejectedBookings = rejectedBookings;
    }

    @Exclude
    public Date getLastBookingDate() {
        return lastBookingDate;
    }

    @Exclude
    public void setLastBookingDate(Date lastBookingDate) {
        this.lastBookingDate = lastBookingDate;
    }

    // Role-based utility methods
    @Exclude
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    @Exclude
    public boolean isFaculty() {
        return "faculty".equalsIgnoreCase(role);
    }

    @Exclude
    public boolean isStudent() {
        return "student".equalsIgnoreCase(role);
    }

    @Exclude
    public boolean canManageLabs() {
        return isAdmin();
    }

    @Exclude
    public boolean canManageBookings() {
        return isAdmin() || isFaculty();
    }

    @Exclude
    public boolean canViewAllBookings() {
        return isAdmin();
    }

    // Utility methods
    @Exclude
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return email != null ? email.split("@")[0] : "Unknown User";
    }

    @Exclude
    public String getFullDisplayInfo() {
        StringBuilder info = new StringBuilder(getDisplayName());

        if (department != null && !department.trim().isEmpty()) {
            info.append(" (").append(department).append(")");
        }

        if (isStudent() && studentId != null && !studentId.trim().isEmpty()) {
            info.append(" - ").append(studentId);
        } else if (!isStudent() && employeeId != null && !employeeId.trim().isEmpty()) {
            info.append(" - ").append(employeeId);
        }

        return info.toString();
    }

    @Exclude
    public boolean isLabRestricted(String labId) {
        return restrictedLabs != null && labId != null && restrictedLabs.contains(labId);
    }

    @Exclude
    public void addSpecialization(String specialization) {
        if (specialization != null && !specialization.trim().isEmpty()) {
            if (this.specializations == null) {
                this.specializations = new ArrayList<>();
            }
            String trimmed = specialization.trim();
            if (!this.specializations.contains(trimmed)) {
                this.specializations.add(trimmed);
            }
        }
    }

    @Exclude
    public void removeSpecialization(String specialization) {
        if (this.specializations != null && specialization != null) {
            this.specializations.remove(specialization.trim());
        }
    }

    @Exclude
    public void addRestrictedLab(String labId) {
        if (labId != null && !labId.trim().isEmpty()) {
            if (this.restrictedLabs == null) {
                this.restrictedLabs = new ArrayList<>();
            }
            if (!this.restrictedLabs.contains(labId)) {
                this.restrictedLabs.add(labId);
            }
        }
    }

    @Exclude
    public void removeRestrictedLab(String labId) {
        if (this.restrictedLabs != null && labId != null) {
            this.restrictedLabs.remove(labId);
        }
    }

    @Exclude
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                role != null && !role.trim().isEmpty();
    }

    @Exclude
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add("Name is required");
        }

        if (email == null || email.trim().isEmpty()) {
            errors.add("Email is required");
        }

        if (role == null || role.trim().isEmpty()) {
            errors.add("Role is required");
        }

        if (isStudent()) {
            if (studentId == null || studentId.trim().isEmpty()) {
                errors.add("Student ID is required for students");
            }
            if (program == null || program.trim().isEmpty()) {
                errors.add("Program is required for students");
            }
        } else if (isFaculty()) {
            if (employeeId == null || employeeId.trim().isEmpty()) {
                errors.add("Employee ID is required for faculty");
            }
            if (department == null || department.trim().isEmpty()) {
                errors.add("Department is required for faculty");
            }
        }

        return errors;
    }

    private void setDefaultLimitsByRole(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            this.maxSimultaneousBookings = 10;
            this.maxWeeklyHours = 40;
            this.canBookWithoutApproval = true;
        } else if ("faculty".equalsIgnoreCase(role)) {
            this.maxSimultaneousBookings = 5;
            this.maxWeeklyHours = 20;
            this.canBookWithoutApproval = false;
        } else { // student
            this.maxSimultaneousBookings = 3;
            this.maxWeeklyHours = 12;
            this.canBookWithoutApproval = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", department='" + department + '\'' +
                ", isActive=" + isActive +
                ", isVerified=" + isVerified +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(email);
        dest.writeString(role);
        dest.writeString(phoneNumber);
        dest.writeString(department);
        dest.writeString(studentId);
        dest.writeString(employeeId);
        dest.writeByte((byte) (isActive ? 1 : 0));
        dest.writeByte((byte) (isVerified ? 1 : 0));
        dest.writeString(profileImageUrl);
        dest.writeString(bio);
        dest.writeStringList(specializations);
        dest.writeString(yearOfStudy);
        dest.writeString(program);
        dest.writeByte((byte) (emailNotifications ? 1 : 0));
        dest.writeByte((byte) (smsNotifications ? 1 : 0));
        dest.writeByte((byte) (pushNotifications ? 1 : 0));
        dest.writeString(preferredLanguage);
        dest.writeString(timezone);
        dest.writeInt(maxSimultaneousBookings);
        dest.writeInt(maxWeeklyHours);
        dest.writeStringList(restrictedLabs);
        dest.writeByte((byte) (canBookWithoutApproval ? 1 : 0));
        dest.writeSerializable(createdAt);
        dest.writeSerializable(updatedAt);
        dest.writeSerializable(lastLoginAt);
    }
}