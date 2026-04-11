# Smart Healthcare System — End-to-End Flow Documentation

Detailed tracing of every feature flow: request path, inter-service calls, rollbacks, fallbacks, and Kafka events.

---

## Table of Contents

1. [Gateway — Request Handling](#1-gateway--request-handling)
2. [User Registration](#2-user-registration)
3. [User Login](#3-user-login)
4. [Role Request — Patient](#4-role-request--patient)
5. [Role Request — Doctor](#5-role-request--doctor)
6. [Role Request — Admin](#6-role-request--admin)
7. [Admin — Approve Role Request](#7-admin--approve-role-request)
8. [Admin — Decline Role Request](#8-admin--decline-role-request)
9. [Book Appointment](#9-book-appointment)
10. [Cancel Appointment — Patient](#10-cancel-appointment--patient)
11. [Doctor — Add Leave](#11-doctor--add-leave)
12. [Complete Appointment](#12-complete-appointment)
13. [Get Prescription](#13-get-prescription)
14. [Doctor Daily Schedule Notification](#14-doctor-daily-schedule-notification)
15. [Doctor Booking Schedule Refresh](#15-doctor-booking-schedule-refresh)
16. [Kafka Topic Reference](#16-kafka-topic-reference)

---

## Conventions

```
→   Synchronous REST call (Feign)
⟶   Kafka publish (fire-and-forget)
✓   Success path
✗   Failure / rollback path
[CB] Circuit Breaker + Retry wraps this call
```

**cancelledBy values:**
| Value | Meaning |
|---|---|
| `PATIENT` | Patient cancelled voluntarily |
| `DOCTOR` | Doctor cancelled via addLeave |
| `SYSTEM` | System rollback (booking failure) |

---

## 1. Gateway — Request Handling

**Every request** passes through the Gateway before reaching any service.

```
Client → POST /api/user-service/open/login
  │
  ├─ Extract X-Correlation-ID header
  │     └─ If missing → generate UUID, attach to request + response
  │
  ├─ Path matches /open/* ?
  │     └─ YES → forward directly, no JWT check ✓
  │
  ├─ Path matches /secure/* ?
  │     ├─ Extract Authorization: Bearer <token>
  │     ├─ Missing or malformed → 401 UNAUTHORIZED ✗
  │     ├─ Invalid/expired JWT → 401 UNAUTHORIZED ✗
  │     └─ Valid JWT →
  │           ├─ Extract email (sub claim) + role claim
  │           ├─ Inject X-User-Email header
  │           ├─ Inject X-User-Role header
  │           └─ Forward to target service ✓
  │
  └─ Service unavailable → /fallback → 503 SERVICE_UNAVAILABLE ✗
```

**Headers injected by Gateway:**
- `X-User-Email` — authenticated user's email
- `X-User-Role` — authenticated user's role (USER / PATIENT / DOCTOR / ADMIN)
- `X-Correlation-ID` — request trace ID

**Downstream services** trust these headers without re-validating JWT.

---

## 2. User Registration

**Endpoint:** `POST /api/user-service/open/newUser`
**Auth:** None (public)
**Role required:** None

```
Client → POST /api/user-service/open/newUser
  │       Body: { userEmail, userPassword, userName, userAge }
  │
  ├─ Validate request body (@Valid)
  │     └─ Validation fails → 400 BAD_REQUEST ✗
  │
  ├─ Check userEmail already exists in MySQL
  │     └─ Exists → 409 CONFLICT ✗
  │
  ├─ Hash password (BCrypt)
  ├─ Set role = USER
  ├─ Save to MySQL
  │
  ├─ [Kafka] ⟶ topic: welcome-notification
  │     Payload: { userEmail, userName, userAge, userRole: "USER" }
  │     └─ notificationService consumes → sends welcome email ✓
  │
  └─ Return 201 UserModel (password nulled out) ✓
```

**Email sent:** Welcome email with account details and next steps.

---

## 3. User Login

**Endpoint:** `POST /api/user-service/open/login`
**Auth:** None (public)

```
Client → POST /api/user-service/open/login
  │       Body: { userEmail, userPassword }
  │
  ├─ AuthenticationManager.authenticate()
  │     └─ Invalid credentials → 401 UNAUTHORIZED (BadCredentialsException) ✗
  │
  ├─ Load user from MySQL
  ├─ Generate JWT (claims: sub=email, role=userRole)
  └─ Return 200 { token, expiration } ✓
```

**JWT expiry:** Configurable via `EXPIRATION` in Vault.env (default 12 hours).

---

## 4. Role Request — Patient

**Endpoint:** `POST /api/user-service/secure/requestPatientAccess`
**Auth:** JWT required
**Role required:** USER, DOCTOR, or PATIENT

```
Client → POST /api/user-service/secure/requestPatientAccess
  │       Body: { patientDto: { dateOfBirth, gender } }
  │       Headers: Authorization: Bearer <token>
  │
  ├─ Validate role is USER/DOCTOR/PATIENT (not ADMIN)
  │     └─ ADMIN → 403 FORBIDDEN ✗
  │
  ├─ Validate patientDto is present
  │     └─ Missing → 400 BAD_REQUEST ✗
  │
  ├─ Set userEmail = X-User-Email (from gateway)
  ├─ Set userRole = PATIENT
  ├─ Set patientDto.email = X-User-Email
  ├─ Fetch userName from MySQL → set patientDto.name
  │
  ├─ [Kafka] ⟶ topic: role-request
  │     Payload: { userEmail, userRole: "PATIENT", patientDto: { name, email, dateOfBirth, gender } }
  │     └─ adminService KafkaListenerAdmin consumes:
  │           ├─ If no existing request → create new RequestRole (PENDING)
  │           └─ If existing → update role + patientDto, reset to PENDING
  │
  └─ Return 202 "Request for Patient access sent successfully" ✓
```

---

## 5. Role Request — Doctor

**Endpoint:** `POST /api/user-service/secure/requestDoctorAccess`
**Auth:** JWT required
**Role required:** USER, DOCTOR, or PATIENT

```
Client → POST /api/user-service/secure/requestDoctorAccess
  │       Body: { doctorDto: { gender, specializations, licenseNumber, contactNumber, overview } }
  │
  ├─ Validate doctorDto is present → 400 if missing ✗
  ├─ Set userEmail, userRole = DOCTOR, doctorDto.email
  │
  ├─ [Kafka] ⟶ topic: role-request
  │     Payload: { userEmail, userRole: "DOCTOR", doctorDto: { email, gender, specializations, ... } }
  │     └─ adminService consumes → saves as PENDING
  │
  └─ Return 202 ✓
```

---

## 6. Role Request — Admin

**Endpoint:** `POST /api/user-service/secure/requestAdminAccess`
**Auth:** JWT required
**Role required:** USER, DOCTOR, or PATIENT

```
Client → POST /api/user-service/secure/requestAdminAccess
  │       Body: { } (no extra data needed)
  │
  ├─ Set userEmail, userRole = ADMIN
  │
  ├─ [Kafka] ⟶ topic: role-request
  │     Payload: { userEmail, userRole: "ADMIN" }
  │     └─ adminService consumes → saves as PENDING
  │
  └─ Return 202 ✓
```

---

## 7. Admin — Approve Role Request

**Endpoint:** `PUT /api/admin-service/secure/approve/{id}?maxCount=4&rate=300`
**Auth:** JWT required
**Role required:** ADMIN

```
Admin → PUT /api/admin-service/secure/approve/{id}
  │
  ├─ Load RequestRole by id
  │     └─ Not found → 404 ✗
  │     └─ Already APPROVED → 400 ✗
  │     └─ Already DISCARDED → 400 ✗
  │
  ├─ [CB] → user-service /secure/changeRole
  │     Body: { email, role: requestedRole }
  │     └─ Fails → 503 (role change reverted not needed, nothing changed yet) ✗
  │
  ├─ If role == DOCTOR:
  │     [CB] → doctor-service /secure/saveDoctor
  │           Params: maxCount, rate
  │           Body: doctorDto
  │           └─ Fails →
  │                 [CB] → user-service /secure/changeRole (rollback to USER)
  │                 → 503 ✗
  │
  ├─ If role == PATIENT:
  │     [CB] → patient-service /secure/savePatient
  │           Body: patientDto
  │           └─ Fails →
  │                 [CB] → user-service /secure/changeRole (rollback to USER)
  │                 → 503 ✗
  │
  ├─ Set requestStatus = APPROVED, save
  │
  ├─ [Kafka] ⟶ topic: role-approved
  │     Payload: { userEmail, userRole }
  │     └─ notificationService → sends approval email ✓
  │
  └─ Return 200 requestId ✓
```

**Bulk Patient Approval:** `POST /api/admin-service/secure/approve/patients`
- Finds all PENDING PATIENT requests
- Processes each with same flow above
- Per-request rollback on failure (does not stop processing others)

---

## 8. Admin — Decline Role Request

**Endpoint:** `PUT /api/admin-service/secure/declineRequest/{id}`
**Auth:** JWT required
**Role required:** ADMIN

```
Admin → PUT /api/admin-service/secure/declineRequest/{id}
  │
  ├─ Load RequestRole by id → 404 if not found ✗
  ├─ Already APPROVED → 400 ✗
  ├─ Already DISCARDED → 400 ✗
  │
  ├─ Set requestStatus = DISCARDED, save
  │
  ├─ [Kafka] ⟶ topic: role-declined
  │     Payload: { userEmail, userRole }
  │     └─ notificationService → sends declined email ✓
  │
  └─ Return 200 requestId ✓
```

---

## 9. Book Appointment

**Endpoint:** `POST /api/patient-service/secure/bookAppointment`
**Auth:** JWT required
**Role required:** PATIENT

```
Patient → POST /api/patient-service/secure/bookAppointment
  │         Body: { doctorId, date, subject, description }
  │
  ├─ Validate patient exists in MongoDB
  │     └─ Not found → 404 ✗
  │
  ├─ [CB] → doctor-service /secure/getDoctorByEmail/{doctorId}
  │     └─ Doctor not found → 404 ✗
  │     └─ Service down → 503 ✗
  │
  ├─ Check slot availability in-memory:
  │     bookingListMap.get(date).availibility == AVAILABLE ?
  │     └─ null / BOOKED / UNAVAILABLE → 500 "Selected slot is not available" ✗
  │         (No appointment saved yet — clean failure, no rollback needed)
  │
  ├─ Set patientId = X-User-Email, status = UPCOMING
  │
  ├─ [CB] → appointment-service /secure/bookAppointment
  │     └─ Saves Appointment to MongoDB
  │     └─ Returns saved AppointmentDto with generated id
  │     └─ Fails → 503 ✗ (nothing saved, clean failure)
  │
  ├─ ── BEGIN ATOMIC BLOCK ──────────────────────────────────────
  │
  ├─ patient.appointmentList.add(appointmentId) → save patient
  │
  ├─ [CB] → doctor-service /secure/addDocAppointment
  │     └─ Adds appointmentId to doctor's slot
  │     └─ Marks slot BOOKED if slot.size >= maxCount
  │
  ├─ ── END ATOMIC BLOCK ────────────────────────────────────────
  │
  │   [IF ANYTHING IN ATOMIC BLOCK FAILS]:
  │     ├─ patient.appointmentList.remove(appointmentId) → save patient
  │     │     └─ Fails → log error (best-effort)
  │     ├─ [CB] → appointment-service /secure/deleteAppointment/{id}
  │     │     └─ Hard delete (no Kafka, no trace left)
  │     │     └─ Fails → log error (best-effort)
  │     └─ Return 500 "Booking failed due to an internal error" ✗
  │
  ├─ [Kafka] ⟶ topic: appointment-booked
  │     Payload: full AppointmentDto map
  │     └─ notificationService → sends booking confirmation to patient + doctor ✓
  │
  └─ Return 201 AppointmentDto ✓
```

**Key design decisions:**
- Slot availability checked before saving appointment — no orphaned records on slot conflict
- `appointment-booked` Kafka event published only after ALL three stores succeed
- Rollback uses hard delete (not cancel) — no Kafka events, no side effects

---

## 10. Cancel Appointment — Patient

**Endpoint:** `DELETE /api/patient-service/secure/cancelAppointment/{id}`
**Auth:** JWT required
**Role required:** PATIENT

```
Patient → DELETE /api/patient-service/secure/cancelAppointment/{id}
  │
  ├─ Validate patient exists → 404 ✗
  ├─ Validate appointment belongs to patient (appointmentList.contains(id))
  │     └─ Not owned → 404 "Appointment does not belong to patient" ✗
  │
  ├─ [CB] → appointment-service /secure/getAppointmentById/{id}
  │     └─ Fetches appointment (need doctorId + date for next step)
  │
  ├─ [CB] → appointment-service /secure/markCancelled/{id}?cancelledBy=<patientEmail>
  │     └─ Sets status = CANCELLED
  │     └─ Sets description = "Appointment cancelled by: <patientEmail>"
  │     └─ NO Kafka event published
  │     └─ Fails → 503 ✗ (nothing changed yet, clean failure)
  │
  ├─ [CB] → doctor-service /secure/removeAppointmentFromSchedule
  │           ?appointmentId=<id>&date=<date>
  │     └─ Removes appointmentId from doctor's slot
  │     └─ If slot was BOOKED → reopens to AVAILABLE
  │     └─ [IF FAILS]:
  │           ├─ [CB] → appointment-service /secure/restoreAppointment/{id}
  │           │     └─ Reverts status to UPCOMING, clears description
  │           │     └─ Fails → log error (best-effort)
  │           └─ Return 500 "Cancellation failed: unable to update doctor schedule" ✗
  │
  ├─ patient.appointmentList.remove(id) → save patient
  │
  ├─ [Kafka] ⟶ topic: appointment-cancelled-notification
  │     Payload: full AppointmentDto map (has patientId, doctorId, date, description)
  │     └─ notificationService → sends cancellation email to patient ✓
  │
  └─ Return 200 AppointmentDto ✓
```

**cancelledBy = patientEmail** — stored in appointment description for audit trail.

---

## 11. Doctor — Add Leave

**Endpoint:** `POST /api/doctor-service/secure/addLeave`
**Auth:** JWT required
**Role required:** DOCTOR
**Body:** `["2026-05-10", "2026-05-11"]` (list of dates)

```
Doctor → POST /api/doctor-service/secure/addLeave
  │         Body: [date1, date2, ...]
  │
  ├─ Validate doctor exists → 404 ✗
  │
  ├─ For each date in leave list:
  │     ├─ Date not in bookingListMap → skip (out of 31-day window)
  │     ├─ Slot already UNAVAILABLE → skip
  │     │
  │     ├─ Snapshot appointmentIds list (copy to avoid mutation during iteration)
  │     ├─ successfullyCleaned = []
  │     │
  │     ├─ For each appointmentId in snapshot:
  │     │
  │     │   STEP 1 — Cancel appointment:
  │     │     [CB] → appointment-service /secure/markCancelled/{id}?cancelledBy=<doctorEmail>
  │     │           └─ Sets status = CANCELLED, description = "Appointment cancelled by: <doctorEmail>"
  │     │           └─ NO Kafka event
  │     │           └─ [FAILS] → log warn, allCleared=false, break inner loop
  │     │
  │     │   STEP 2 — Remove from patient list:
  │     │     [CB] → patient-service /secure/removeAppointment/{appointmentId}
  │     │           └─ Finds patient by appointmentId, removes from appointmentList
  │     │           └─ [FAILS]:
  │     │                 ├─ [CB] → appointment-service /secure/restoreAppointment/{id}
  │     │                 │     └─ Reverts to UPCOMING (rollback step 1)
  │     │                 │     └─ Fails → log error (best-effort)
  │     │                 └─ allCleared=false, break inner loop
  │     │
  │     │   BOTH STEPS SUCCEEDED:
  │     │     ├─ successfullyCleaned.add(appointmentId)
  │     │     └─ [Kafka] ⟶ topic: appointment-cancelled-notification
  │     │           Payload: { appointmentId, cancelledBy: doctorEmail, date }
  │     │           └─ notificationService → sends cancellation email to patient ✓
  │     │
  │     ├─ slot.bookingId.removeAll(successfullyCleaned)
  │     └─ allCleared == true → slot.availibility = UNAVAILABLE
  │        allCleared == false → slot stays as-is (partial, logged as warn)
  │
  ├─ Save doctor with updated bookingListMap
  └─ Return 200 DoctorDto ✓
```

**Key design decisions:**
- Per-appointment atomicity: each appointment is independently cancelled + patient-removed
- If patient removal fails, appointment is restored (no orphaned cancelled appointment)
- Slot only marked UNAVAILABLE if ALL appointments on that date were successfully processed
- Successfully cleaned appointments are removed from slot regardless of overall allCleared status
- Kafka notification sent per appointment immediately after both steps succeed

---

## 12. Complete Appointment

**Endpoint:** `POST /api/doctor-service/secure/completeAppointment`
**Auth:** JWT required
**Role required:** DOCTOR
**Body:** `{ appointmentId, healthCheck: { height, weight, bpSys, bpDia, oxyLvl, bloodSugar, heartRate, bodyTemperature, respiratoryRate, bmi }, prescription }`

```
Doctor → POST /api/doctor-service/secure/completeAppointment
  │
  ├─ Validate @Valid on VisitDetails → healthCheck fields validated ✓
  │
  ├─ [CB] → appointment-service /secure/getAppointmentById/{appointmentId}
  │     └─ Not found → 404 ✗
  │
  ├─ Verify appointment.doctorId == X-User-Email
  │     └─ Mismatch → 404 DoctorNotFoundException ✗
  │
  ├─ [CB] → appointment-service /secure/completeAppointment
  │     Body: { appointmentId, healthCheck, prescription }
  │     └─ Sets status = VISITED
  │     └─ Saves visitDetails (healthCheck + prescription)
  │     └─ [Kafka] ⟶ topic: appointment-completed
  │           Payload: full Appointment entity map (includes visitDetails)
  │           Consumers:
  │             ├─ patientService KafkaListenerPatient:
  │             │     → extracts healthCheck from visitDetails
  │             │     → patient.vitalsFlow.put(date, healthCheck) → save
  │             └─ notificationService:
  │                   → sends visit completion email to patient ✓
  │
  └─ Return 200 AppointmentDto ✓
```

**HealthCheck validation constraints:**
- height: 50–300 cm
- weight: 2.0–500.0 kg
- bpSys: 70–250 mmHg, bpDia: 40–150 mmHg
- oxyLvl: 50–100%, bloodSugar: 40–600 mg/dL
- heartRate: 30–220 bpm, bodyTemperature: 30.0–45.0 °C
- respiratoryRate: 5–60 breaths/min (optional)
- bmi: calculated field (optional)

---

## 13. Get Prescription

**Endpoint:** `GET /api/patient-service/secure/getPrescription/{id}`
**Auth:** JWT required
**Role required:** PATIENT

```
Patient → GET /api/patient-service/secure/getPrescription/{id}
  │
  ├─ [CB] → appointment-service /secure/getAppointmentById/{id}
  │
  ├─ Check visitDetails != null AND appointment.patientId == X-User-Email
  │     └─ No visitDetails or not owner → return "No prescription available" (200) ✓
  │
  ├─ [Kafka] ⟶ topic: send-email-appointment
  │     Payload: full AppointmentDto map (includes visitDetails with healthCheck + prescription)
  │     └─ notificationService:
  │           ├─ Renders prescription-email.html (HTML email body)
  │           ├─ Renders prescription-pdf.html → generates PDF via Flying Saucer
  │           └─ Sends email with PDF attachment to patient ✓
  │
  └─ Return 202 "Prescription for Appointment has been sent out to Email" ✓
```

---

## 14. Doctor Daily Schedule Notification

**Trigger:** Scheduler in doctorService — runs daily at **00:05**

```
Scheduler → sendDailyScheduleNotification() [cron: 0 5 0 * * *]
  │
  ├─ Calculate tomorrow = LocalDate.now().plusDays(1)
  ├─ Load all doctors from MongoDB
  │
  ├─ For each doctor:
  │     ├─ Get slot = bookingListMap.get(tomorrow)
  │     ├─ slot == null or slot.bookingId.isEmpty() → skip
  │     └─ [Kafka] ⟶ topic: doctor-daily-schedule
  │           Payload: { doctorEmail, date, appointmentIds: [...] }
  │           └─ notificationService:
  │                 → renders doctor-daily-schedule-email.html
  │                 → sends email listing all appointment IDs for tomorrow ✓
  │
  └─ Logs total doctors processed ✓
```

---

## 15. Doctor Booking Schedule Refresh

**Trigger:** Scheduler in doctorService — runs daily at **00:00**

```
Scheduler → refreshBookingSchedules() [cron: 0 0 0 * * *]
  │
  ├─ Load all doctors from MongoDB
  ├─ today = LocalDate.now()
  │
  ├─ For each doctor:
  │     ├─ Remove all entries where date < today (past dates)
  │     ├─ For i = 0 to 30:
  │     │     day = today + i days
  │     │     if day not in bookingListMap:
  │     │         add day with availibility = AVAILABLE
  │     └─ Save doctor
  │
  └─ Maintains a rolling 31-day window for every doctor ✓
```

**Note:** Runs 5 minutes before `sendDailyScheduleNotification` to ensure the schedule is up-to-date before notifications are sent.

---

## 16. Kafka Topic Reference

### Topics and Payloads

#### `welcome-notification`
**Publisher:** userService (AuthService.addNewUser)
**Consumer:** notificationService
```json
{
  "userEmail": "user@example.com",
  "userName": "John Doe",
  "userAge": 28,
  "userRole": "USER"
}
```

#### `role-request`
**Publisher:** userService (UserService — all 3 role request methods)
**Consumer:** adminService (KafkaListenerAdmin → saves as PENDING)
```json
{
  "userEmail": "user@example.com",
  "userRole": "PATIENT",
  "patientDto": { "name": "John", "email": "user@example.com", "dateOfBirth": "1995-01-01", "gender": "MALE" }
}
```

#### `role-approved`
**Publisher:** adminService (AdminService.approveRequest + approvePatientRequest)
**Consumer:** notificationService
```json
{ "userEmail": "user@example.com", "userRole": "PATIENT" }
```

#### `role-declined`
**Publisher:** adminService (AdminService.declineRequest)
**Consumer:** notificationService
```json
{ "userEmail": "user@example.com", "userRole": "DOCTOR" }
```

#### `appointment-booked`
**Publisher:** patientService (PatientService.bookAppointment — after full success)
**Consumer:** notificationService
```json
{
  "id": "appt-123",
  "patientId": "patient@example.com",
  "doctorId": "doctor@example.com",
  "status": "UPCOMING",
  "subject": "General Checkup",
  "date": "2026-05-10"
}
```

#### `appointment-cancelled-notification`
**Publisher:** patientService (cancelAppointment) + doctorService (addLeave)

Patient cancel payload (full AppointmentDto):
```json
{
  "id": "appt-123",
  "patientId": "patient@example.com",
  "doctorId": "doctor@example.com",
  "date": "2026-05-10",
  "status": "CANCELLED",
  "description": "Appointment cancelled by: patient@example.com"
}
```

Doctor addLeave payload (minimal):
```json
{
  "appointmentId": "appt-123",
  "cancelledBy": "doctor@example.com",
  "date": "2026-05-10"
}
```
> Note: notificationService handles both shapes — uses `patientId` if present, falls back to `appointmentId` key.

#### `appointment-completed`
**Publisher:** appointmentService (AppointmentService.completeAppointment)
**Consumers:** patientService (updates vitals), notificationService (sends completion email)
```json
{
  "id": "appt-123",
  "patientId": "patient@example.com",
  "doctorId": "doctor@example.com",
  "date": "2026-05-10",
  "status": "VISITED",
  "visitDetails": {
    "appointmentId": "appt-123",
    "healthCheck": { "height": 175, "weight": 70.5, "bpSys": 120, "bpDia": 80, ... },
    "prescription": "Take Paracetamol 500mg twice daily for 5 days"
  }
}
```

#### `send-email-appointment`
**Publisher:** patientService (PatientService.getPrescription)
**Consumer:** notificationService (sends prescription email with PDF attachment)
```json
{
  "id": "appt-123",
  "patientId": "patient@example.com",
  "doctorId": "doctor@example.com",
  "date": "2026-05-10",
  "subject": "General Checkup",
  "status": "VISITED",
  "visitDetails": { "healthCheck": { ... }, "prescription": "..." }
}
```

#### `doctor-daily-schedule`
**Publisher:** doctorService (DoctorService.sendDailyScheduleNotification — scheduler)
**Consumer:** notificationService
```json
{
  "doctorEmail": "doctor@example.com",
  "date": "2026-05-11",
  "appointmentIds": ["appt-123", "appt-456", "appt-789"]
}
```

---

## Rollback Summary

| Flow | Trigger | Rollback Actions |
|---|---|---|
| Book Appointment | patient/doctor update fails | delete appointment + remove from patient list |
| Cancel Appointment (patient) | doctor schedule update fails | restore appointment to UPCOMING |
| Add Leave (per appointment) | patient remove fails | restore appointment to UPCOMING |
| Approve Role Request | profile save fails | revert user role to USER |

---

**For infrastructure and deployment details, see [INFRASTRUCTURE.md](INFRASTRUCTURE.md)**
**For general project overview, see [README.md](README.md)**
