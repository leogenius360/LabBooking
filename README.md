# Lab Booking System

A **Java-based Android application** integrated with **Firebase** for managing IT laboratory reservations within a university environment. This system is designed to streamline lab booking operations for both students and administrators, ensuring an efficient and transparent scheduling process.

---

## ✨ Features
- **Secure User Authentication** – Powered by Firebase Authentication for role-based (student/admin) login.
- **Comprehensive Lab Management** – View lab details including capacity, equipment, and availability.
- **Intelligent Booking Management** – Reserve labs for specified time slots with conflict prevention.
- **Admin Control Panel** – Approve, reject, modify, and oversee all bookings.
- **Real-time Synchronization** – Firestore integration ensures instant updates across devices.
- **Push Notifications** – Firebase Cloud Messaging keeps users informed on booking status changes.
- **Usage Analytics** – Firebase Analytics for monitoring booking trends and lab utilization.

---

## 🛠 Tech Stack
- **Language:** Java
- **Framework:** Android SDK
- **Backend Services:** Firebase (Authentication, Firestore, Cloud Messaging, Analytics)
- **UI Framework:** Material Design Components (MD3)

---

## 📂 Project Structure
```
app/src/main/java/com/geniustechspace/labbooking/
├── models/
│   ├── User.java
│   ├── Lab.java
│   └── Booking.java
├── utils/
│   ├── DatabaseUtils.java
│   ├── AuthUtils.java
│   └── DateTimeUtils.java
├── adapters/
│   ├── LabsAdapter.java
│   └── BookingsAdapter.java
├── activities/
│   ├── MainActivity.java
│   ├── LoginActivity.java
│   ├── DashboardActivity.java
│   ├── BookingActivity.java
│   └── AdminActivity.java
├── viewmodels/
│   ├── LabViewModel.java
│   └── BookingViewModel.java
└── repositories/
    ├── LabRepository.java
    └── BookingRepository.java

app/src/main/res/
├── layout/
│   ├── activity_main.xml
│   ├── activity_login.xml
│   ├── activity_dashboard.xml
│   ├── activity_booking.xml
│   ├── activity_admin.xml
│   ├── item_lab.xml
│   └── item_booking.xml
├── values/
│   ├── strings.xml
│   ├── colors.xml
│   └── styles.xml
└── drawable/
```

---

## 🚀 Installation & Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/DominicMaabobra/lab-booking.git
   cd lab-booking
   ```
2. **Open in Android Studio.**
3. **Integrate Firebase:**
   - Navigate to **Tools > Firebase**.
   - Connect to your Firebase project.
   - Enable **Authentication**, **Cloud Firestore**, and **Cloud Messaging**.
4. **Add `google-services.json`** to the `app/` directory.
5. **Build & Run** on an emulator or a physical device.

---

## 🔧 Firebase Configuration
- **Authentication Provider:** Email/Password
- **Firestore Collections:**
  - `users` – Stores user profiles & roles.
  - `labs` – Contains lab details & availability.
  - `bookings` – Tracks reservations & statuses.
- **Cloud Messaging:** Sends booking confirmations and updates.

---

## 🤝 Contributing
We welcome contributions from the community:
1. Fork the repository.
2. Create a dedicated branch for your feature or fix.
3. Commit with clear and descriptive messages.
4. Open a pull request for review.

---

## 📜 License
This project is licensed under the **MIT License**.

---
**Author:** Dominic Maabobra Tuolong  
**Organization:** Genius Tech Space
