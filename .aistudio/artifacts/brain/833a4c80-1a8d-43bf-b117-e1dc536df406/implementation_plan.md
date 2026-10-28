# SAVE TRACK - Native Android Logistics & Live Parcel Tracking System

A production-grade native Android logistics and real-time parcel delivery ecosystem built with Kotlin and Jetpack Compose. SAVE TRACK provides three authenticated user roles (Company Manager, Delivery Rider, and Parcel Customer), verifiable rider shift attendance with an in-app CameraX front-camera selfie, live GPS location tracking with a foreground service, interactive map navigation, and end-to-end parcel lifecycle management backed by Cloud Firestore.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Decisions from Clarifications:**
> - **Authentication Strategy**: Google Sign-In via Jetpack Credential Manager as the verified identity provider (in strict compliance with platform requirements), immediately linked to a verified Firestore profile (`/users/{uid}`) storing the user's role (`MANAGER`, `RIDER`, `CUSTOMER`), corporate email, status, and company credentials.
> - **Live Map Tracking**: Google Maps integration with API key configuration in Secrets (`.env` / `BuildConfig`), paired with an interactive native fallback canvas map rendering real GPS coordinates, route vectors, and telemetry when an API key is not yet configured.
> - **Shift-Start Attendance Selfie**: In-app front-facing **CameraX** live capture with real-time preview, biometric/pose guide overlay, image capture, and audit record generation stored in Firestore with server timestamps.
> - **Zero Mock Data Policy**: Production database initialized empty. Clean, actionable empty states prompt real account registration, parcel creation, and shift start.

---

### 1. Overview & Core Concept

**SAVE TRACK** is an enterprise-grade native Android logistics solution designed to eliminate lost parcels, ensure driver accountability, and provide transparent real-time package delivery tracking.

- **Target Personas**:
  1. **Company Manager / Office Administrator**: Creates parcels, manages riders, approves rider accounts, inspects shift attendance selfies, assigns parcels to riders, monitors active riders on a live dispatch map, and resolves delivery exceptions.
  2. **Delivery Rider**: Starts shifts by capturing a mandatory live front-camera selfie, views assigned parcels in real time, triggers GPS tracking via a foreground service, navigates routes, and transitions deliveries through states (*Picked Up*, *In Transit*, *Arrived*, *Delivered*, or *Failed* with reason).
  3. **Parcel Customer**: Securely tracks their specific parcels using authenticated ownership or tracking numbers, monitoring live rider coordinates and estimated arrival during active delivery windows.
- **Key Value**: Direct operational transparency, verifiable attendance through camera capture, zero simulated data, and strict role-based access control.

---

### 2. User Experience & Visual Design

#### Visual Identity & Design System
- **Brand Aesthetic**: Modern high-tech logistics. Deep, authoritative navy blues contrasted with high-visibility electric cyan/teal accents.
- **Color Palette**:
  - `Primary / Brand`: Midnight Navy (`#0A192F` and `#0F172A`)
  - `Tracking Accent`: Electric Cyan (`#00E5FF` / `#00B4D8`)
  - `Delivery Success`: Emerald Green (`#10B981`)
  - `Alert / Attention`: Amber Gold (`#F59E0B`)
  - `Error / Failed`: Crimson (`#EF4444`)
  - `Background & Surfaces`: Crisp Off-White / Soft Charcoal (`#F8FAFC` in light mode, `#0B1120` in dark mode)
- **Typography**: Clean, geometric sans-serif styling with prominent tracking numbers, mono-styled status tags, and accessible contrast.
- **Iconography**: Custom adaptive app icon combining a secure shield, location pin, and delivery parcel.

#### Key User Flows
1. **Authentication & Role Onboarding Flow**:
   - Splash screen with original SAVE TRACK shield/pin logo.
   - Google Sign-In authentication gate with Credential Manager.
   - Automatic role detection: If new user, interactive role selection (Manager, Rider, Customer) with registration fields. Managers require company code / admin status; Riders start in `PENDING` status until approved by a Manager.
2. **Rider Shift-Start & Live Selfie Flow**:
   - Rider dashboard displays current shift state (`OFF_DUTY` or `ON_DUTY`).
   - Tapping **"Start Shift"** requests `CAMERA`, `ACCESS_FINE_LOCATION`, and `POST_NOTIFICATIONS` permissions.
   - Launches in-app **CameraX** front-facing viewfinder with oval face alignment guide.
   - Rider captures a live selfie; image is processed into a protected shift audit record.
   - Shift status updates to `ACTIVE` in Firestore; Android Tracking Foreground Service starts with persistent notification.
3. **Manager Parcel & Dispatch Flow**:
   - Manager navigates tabs: **Live Dispatch Map**, **Parcels**, **Riders & Shifts**, and **Analytics**.
   - Create Parcel form: Tracking ID generator, sender/recipient info, addresses, coordinates, priority, and notes.
   - Instant assignment to an active or verified rider with real-time Firestore sync.
   - Shift audit tab: View riders' shift-start selfies, timestamps, and active working hours.
4. **Customer Live Tracking Flow**:
   - Customer enters tracking ID or views parcels linked to their email/account.
   - Live status stepper: Order Placed → Picked Up → In Transit → Out for Delivery → Delivered.
   - Real-time map displaying current rider location (only during active delivery), destination pin, and delivery milestones.

---

### 3. Key Product Decisions & Trade-Offs

1. **CameraX In-App Viewfinder vs. External Camera Intent**:
   - *Chosen Approach*: Embedded CameraX `PreviewView` with front-facing lens selector and capture button.
   - *Why*: Eliminates gallery manipulation, ensures live capture in the app, and provides an immediate, polished UI with face framing guides.
2. **Foreground Location Tracking Service**:
   - *Chosen Approach*: Dedicated Android `ForegroundService` with `FOREGROUND_SERVICE_TYPE_LOCATION`, persistent notification, and `FusedLocationProviderClient`.
   - *Why*: Guarantees reliable continuous GPS updates even when the rider switches to external navigation apps (Google Maps / Waze).
3. **Map Display Architecture with Dual Mode**:
   - *Chosen Approach*: Built-in Map component supporting Google Maps SDK tiles when `MAPS_API_KEY` is present in `BuildConfig`, seamlessly falling back to a custom interactive vector map that plots real GPS coordinates, heading markers, and pickup/destination waypoints.
   - *Why*: Ensures the application functions reliably in all testing environments regardless of external Google Cloud billing setup, while ready for instant production Maps SDK display.
4. **Firestore Zero-Trust Security Rules**:
   - *Chosen Approach*: Schema validation functions, role-based checks (`request.auth.uid == resource.data.riderId`, `isManager()`), and server timestamps.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          SAVE TRACK ANDROID APP                        │
├────────────────────────────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose M3 + Navigation Compose)                    │
│  ┌──────────────────┐ ┌──────────────────┐ ┌─────────────────────────┐ │
│  │ ManagerDashboard │ │  RiderDashboard  │ │    CustomerTracking     │ │
│  │ - Dispatch Map   │ │  - Shift Control │ │    - Live Status        │ │
│  │ - Parcel Manager │ │  - CameraX Selfie│ │    - Rider Location Map │ │
│  │ - Shift Review   │ │  - Assigned List │ │    - Issue Report       │ │
│  └─────────▲────────┘ └────────▲─────────┘ └────────────▲────────────┘ │
│            │                   │                        │              │
│  ViewModel & State Layer (Coroutines, StateFlow, MVVM)                 │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │ ManagerViewModel / RiderViewModel / TrackingViewModel / AuthVm    │ │
│  └───────────────────────────────▲───────────────────────────────────┘ │
│                                  │                                     │
│  Domain & Service Layer                                                │
│  ┌───────────────────────────────┴───────────────────────────────────┐ │
│  │ - LocationTrackingService (Foreground Service + Notification)      │ │
│  │ - CameraCaptureManager (CameraX Lifecycle + Image Capture)        │ │
│  │ - RoleAuthorizationManager (Role Verification & Security Checks)   │ │
│  └───────────────────────────────▲───────────────────────────────────┘ │
│                                  │                                     │
│  Data Layer & Repositories                                             │
│  ┌───────────────────────────────┴───────────────────────────────────┐ │
│  │ ParcelRepository | ShiftRepository | UserRepository | LocationRepo │ │
│  └───────────────────────────────▲───────────────────────────────────┘ │
└──────────────────────────────────┼─────────────────────────────────────┘
                                   │
              Firebase Cloud Firestore & Jetpack Credential Manager
              - Collections: /users, /parcels, /shifts, /tracking_logs
              - Rules: Hardened zero-trust security rules
```

#### Core Data Entities in Firestore
1. **User Profile (`/users/{uid}`)**:
   - `uid: String`, `name: String`, `email: String`, `role: "MANAGER" | "RIDER" | "CUSTOMER"`, `status: "ACTIVE" | "PENDING" | "SUSPENDED"`, `phoneNumber: String`, `createdAt: Timestamp`.
2. **Parcel (`/parcels/{parcelId}`)**:
   - `id: String`, `trackingNumber: String`, `title: String`, `senderAddress: String`, `deliveryAddress: String`, `pickupLat: Double`, `pickupLng: Double`, `destLat: Double`, `destLng: Double`, `recipientName: String`, `recipientPhone: String`, `riderId: String?`, `riderName: String?`, `status: "CREATED" | "ASSIGNED" | "PICKED_UP" | "IN_TRANSIT" | "ARRIVED" | "DELIVERED" | "FAILED"`, `failureReason: String?`, `priority: "NORMAL" | "EXPRESS" | "URGENT"`, `notes: String`, `createdAt: Timestamp`, `updatedAt: Timestamp`.
3. **Shift Attendance (`/shifts/{shiftId}`)**:
   - `id: String`, `riderId: String`, `riderName: String`, `selfieBase64: String`, `startTime: Timestamp`, `endTime: Timestamp?`, `status: "ACTIVE" | "COMPLETED"`, `lastLat: Double`, `lastLng: Double`, `deviceInfo: String`.
4. **Live Location Logs (`/tracking_logs/{riderId}`)**:
   - `riderId: String`, `latitude: Double`, `longitude: Double`, `accuracy: Float`, `speed: Float`, `heading: Float`, `timestamp: Timestamp`.

---

### 5. Implementation Stages & Build Verification

- **Stage 1**: Dependencies & Manifest setup (CameraX, Play Services Location, Navigation, Firebase, Foreground Service permissions).
- **Stage 2**: Theme, Brand Design, Adaptive App Icon, and Logo creation.
- **Stage 3**: Firebase Firestore & Jetpack Credential Manager Auth Layer (`UserRepository`, `firebase-blueprint.json`, `firestore.rules`).
- **Stage 4**: In-app CameraX live selfie capture composable and Shift Attendance subsystem.
- **Stage 5**: Real-time GPS Location Tracking Service with foreground notification.
- **Stage 6**: Three distinct dashboards:
  - Manager Dashboard (Dispatch Map, Parcel Creator, Rider Management, Shift Review).
  - Rider Dashboard (Shift Start/End, Live Selfie, Assigned Parcels, Step-by-Step Delivery Actions).
  - Customer Tracking View (Live delivery timeline, Map tracking, Delivery confirmation).
- **Stage 7**: Full compilation and verification via `compile_applet`.
