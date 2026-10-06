# Clinic CRUD foundation — Postman testing

Start PostgreSQL and ensure the existing `clinic` database is available, then run `mvn spring-boot:run` (Java 21). Base URL: `http://localhost:8080`.

For every POST/PUT request use Body → raw → JSON and `Content-Type: application/json`. Create the records in the order below. Omit `id` when creating. Each create returns **201 Created**, the saved entity, and a generated UUID in `id`. Copy that UUID to the specified Postman environment variable. Double braces below are Postman variables, not literal UUIDs.

JSON uses camelCase properties. Appointment relationships use `doctor: {"id": "..."}` and `patient: {"id": "..."}`; Doctor uses `office: {"id": "..."}`. There are no DTO fields such as `doctorId` or `patientId`. Relationship references are looked up before saving.

## 1. Offices

Send each body separately to **POST http://localhost:8080/api/office/save**.
Expected for each: **201**, for example `{"id":"<generated UUID>","name":"Cardiology Office","officeNumber":101}`.

Save returned `id` as `office101`.

```json
{
  "name": "Cardiology Office",
  "officeNumber": 101
}
```

Save returned `id` as `office102`.

```json
{
  "name": "Pediatrics Office",
  "officeNumber": 102
}
```

Save returned `id` as `office103`.

```json
{
  "name": "Dermatology Office",
  "officeNumber": 103
}
```

## 2. Specializations

Send each body separately to **POST http://localhost:8080/api/specializations/save**.
Expected for each: **201**, `{"id":"<generated UUID>","name":"<submitted name>"}`.

Save returned `id` as `cardiology`.

```json
{
  "name": "Cardiology"
}
```

Save returned `id` as `pediatrics`.

```json
{
  "name": "Pediatrics"
}
```

Save returned `id` as `dermatology`.

```json
{
  "name": "Dermatology"
}
```

Save returned `id` as `neurology`.

```json
{
  "name": "Neurology"
}
```

## 3. Doctors

Send each body separately to **POST http://localhost:8080/api/doctor/save**.
Expected for each: **201**, the submitted name and date of birth, a generated `id`, and the resolved `office` containing its UUID, name and officeNumber. The ignored `appointments` and `specializations` collections are omitted to prevent recursive JSON. Each office can belong to only one doctor, preserving the existing one-to-one relationship.

Save returned `id` as `doctor1`.

```json
{
  "firstName": "Jean",
  "lastName": "Niyonzima",
  "dateOfBirth": "1985-06-15",
  "office": {
    "id": "{{office101}}"
  }
}
```

Save returned `id` as `doctor2`.

```json
{
  "firstName": "Alice",
  "lastName": "Mutesi",
  "dateOfBirth": "1990-03-20",
  "office": {
    "id": "{{office102}}"
  }
}
```

Save returned `id` as `doctor3`.

```json
{
  "firstName": "Eric",
  "lastName": "Habimana",
  "dateOfBirth": "1982-09-12",
  "office": {
    "id": "{{office103}}"
  }
}
```

### Assign specializations using the existing owning side

Send these four **POST** requests with **no body**:

| URL | Expected response |
| --- | --- |
| http://localhost:8080/api/specializations/{{cardiology}}/doctors/{{doctor1}} | 200, Cardiology entity with id and name |
| http://localhost:8080/api/specializations/{{dermatology}}/doctors/{{doctor1}} | 200, Dermatology entity with id and name |
| http://localhost:8080/api/specializations/{{pediatrics}}/doctors/{{doctor2}} | 200, Pediatrics entity with id and name |
| http://localhost:8080/api/specializations/{{dermatology}}/doctors/{{doctor3}} | 200, Dermatology entity with id and name |

Doctor 1 has two specializations. **Do not assign Neurology**. Repeating an assignment returns 200 without adding a second link. Doctor specialization collections and Specialization doctor collections are ignored in JSON; verify assignments in the PostgreSQL `doctor_specialization` table.

## 4. Patients

Send each body separately to **POST http://localhost:8080/api/patient/save**.
Expected for each: **201**, `{"id":"<generated UUID>","firstName":"<submitted firstName>","lastName":"<submitted lastName>","dateOfBirth":"<submitted date>"}`.

Save returned `id` as `patient1`.

```json
{
  "firstName": "Grace",
  "lastName": "Uwase",
  "dateOfBirth": "2000-08-10"
}
```

Save returned `id` as `patient2`.

```json
{
  "firstName": "Diane",
  "lastName": "Uwase",
  "dateOfBirth": "1998-02-18"
}
```

Save returned `id` as `patient3`.

```json
{
  "firstName": "Patrick",
  "lastName": "Mugisha",
  "dateOfBirth": "1995-04-22"
}
```

Save returned `id` as `patient4`.

```json
{
  "firstName": "Aline",
  "lastName": "Mukamana",
  "dateOfBirth": "2002-11-05"
}
```

Save returned `id` as `patient5`.

```json
{
  "firstName": "Samuel",
  "lastName": "Ishimwe",
  "dateOfBirth": "1989-07-30"
}
```

## 5. Ten appointments

Send each body separately to **POST http://localhost:8080/api/appointments/save**.
Expected for each successful save: **201**, a generated `id`, the submitted appointmentDate/reason/status, the resolved `patient` (UUID, names, dateOfBirth), and resolved `doctor` (UUID, names, dateOfBirth, office). Collections are omitted. Save returned IDs as `appointment1` through `appointment10`.

**Part A seed compatibility:** These are the original foundation seed bodies. With A4 enabled, Appointment 2 returns **409** because Appointment 1 already books Doctor 1 on October 5. When seeding a fresh database now, change Appointment 2's `appointmentDate` to `2026-10-06` to create all ten records successfully. Existing foundation data is retained; do not re-submit it. The original same-day pair can only remain as previously seeded data.

Dates are date-only values in October and November 2026. The only valid statuses are SCHEDULED, CONFIRMED, COMPLETED and CANCELLED.

Appointment 1 — store `appointment1`.

```json
{
  "appointmentDate": "2026-10-05",
  "reason": "Cardiology consultation",
  "status": "SCHEDULED",
  "patient": {
    "id": "{{patient1}}"
  },
  "doctor": {
    "id": "{{doctor1}}"
  }
}
```

Appointment 2 — store `appointment2`.

```json
{
  "appointmentDate": "2026-10-05",
  "reason": "Blood pressure follow-up",
  "status": "CONFIRMED",
  "patient": {
    "id": "{{patient2}}"
  },
  "doctor": {
    "id": "{{doctor1}}"
  }
}
```

Appointment 3 — store `appointment3`.

```json
{
  "appointmentDate": "2026-10-12",
  "reason": "Cardiology follow-up",
  "status": "COMPLETED",
  "patient": {
    "id": "{{patient1}}"
  },
  "doctor": {
    "id": "{{doctor1}}"
  }
}
```

Appointment 4 — store `appointment4`.

```json
{
  "appointmentDate": "2026-10-18",
  "reason": "Pediatric consultation",
  "status": "CANCELLED",
  "patient": {
    "id": "{{patient3}}"
  },
  "doctor": {
    "id": "{{doctor2}}"
  }
}
```

Appointment 5 — store `appointment5`.

```json
{
  "appointmentDate": "2026-10-25",
  "reason": "Skin consultation",
  "status": "CONFIRMED",
  "patient": {
    "id": "{{patient4}}"
  },
  "doctor": {
    "id": "{{doctor3}}"
  }
}
```

Appointment 6 — store `appointment6`.

```json
{
  "appointmentDate": "2026-11-02",
  "reason": "Cardiology review",
  "status": "SCHEDULED",
  "patient": {
    "id": "{{patient1}}"
  },
  "doctor": {
    "id": "{{doctor1}}"
  }
}
```

Appointment 7 — store `appointment7`.

```json
{
  "appointmentDate": "2026-11-08",
  "reason": "Pediatric follow-up",
  "status": "COMPLETED",
  "patient": {
    "id": "{{patient3}}"
  },
  "doctor": {
    "id": "{{doctor2}}"
  }
}
```

Appointment 8 — store `appointment8`.

```json
{
  "appointmentDate": "2026-11-15",
  "reason": "Skin follow-up",
  "status": "CANCELLED",
  "patient": {
    "id": "{{patient5}}"
  },
  "doctor": {
    "id": "{{doctor3}}"
  }
}
```

Appointment 9 — store `appointment9`.

```json
{
  "appointmentDate": "2026-11-20",
  "reason": "General consultation",
  "status": "CONFIRMED",
  "patient": {
    "id": "{{patient2}}"
  },
  "doctor": {
    "id": "{{doctor2}}"
  }
}
```

Appointment 10 — store `appointment10`.

```json
{
  "appointmentDate": "2026-11-27",
  "reason": "Dermatology review",
  "status": "SCHEDULED",
  "patient": {
    "id": "{{patient4}}"
  },
  "doctor": {
    "id": "{{doctor3}}"
  }
}
```

The seed creates 3 Offices, 4 Specializations, 3 Doctors, 5 Patients, and 10 Appointments. Patient 1 has three appointments (1, 3, 6); Doctor 1 has two appointments on October 5 (1, 2); two Patients have lastName Uwase. All four statuses occur, and Neurology has no doctor. Every Doctor has an Office.

## 6. Read, update and delete CRUD

Use these exact URLs, replacing the variables with the IDs captured above:

| Entity | GET all (200, JSON array) | GET one (200, entity) | PUT (200, updated entity) | DELETE (200, message) |
| --- | --- | --- | --- | --- |
| Office | http://localhost:8080/api/office/all | http://localhost:8080/api/office/{{office101}} | http://localhost:8080/api/office/update/{{office101}} | http://localhost:8080/api/office/delete/{{office101}} |
| Doctor | http://localhost:8080/api/doctor/all | http://localhost:8080/api/doctor/{{doctor1}} | http://localhost:8080/api/doctor/update/{{doctor1}} | http://localhost:8080/api/doctor/delete/{{doctor1}} |
| Patient | http://localhost:8080/api/patient/all | http://localhost:8080/api/patient/{{patient1}} | http://localhost:8080/api/patient/update/{{patient1}} | http://localhost:8080/api/patient/delete/{{patient1}} |
| Specialization | http://localhost:8080/api/specializations/all | http://localhost:8080/api/specializations/{{cardiology}} | http://localhost:8080/api/specializations/update/{{cardiology}} | http://localhost:8080/api/specializations/delete/{{cardiology}} |
| Appointment | http://localhost:8080/api/appointments/all | http://localhost:8080/api/appointments/{{appointment1}} | http://localhost:8080/api/appointments/update/{{appointment1}} | http://localhost:8080/api/appointments/delete/{{appointment1}} |

GET and DELETE have no body. A missing UUID returns **404**. The all endpoints return `[]` when empty. Delete returns `Office deleted successfully`, `Doctor deleted successfully`, `Patient deleted successfully`, `Specialization deleted successfully` or `Appointment deleted successfully`.

PUT replaces editable scalar fields and, for Doctor/Appointment, their references. Include the full body below. The URL selects the record; omit body id. Perform these after observing the original seed data.

### Office PUT body
```json
{
  "name": "Cardiology Consultation Office",
  "officeNumber": 101
}
```

### Doctor PUT body
```json
{
  "firstName": "Jean",
  "lastName": "Niyonzima",
  "dateOfBirth": "1985-06-15",
  "office": {
    "id": "{{office101}}"
  }
}
```

### Patient PUT body
```json
{
  "firstName": "Grace",
  "lastName": "Uwase",
  "dateOfBirth": "2000-08-10"
}
```

### Specialization PUT body
```json
{
  "name": "Cardiology"
}
```

### Appointment PUT body
```json
{
  "appointmentDate": "2026-10-05",
  "reason": "Cardiology consultation updated",
  "status": "CONFIRMED",
  "patient": {
    "id": "{{patient1}}"
  },
  "doctor": {
    "id": "{{doctor1}}"
  }
}
```

### Constraint checks

- POST Office with officeNumber 101 again: **409**. PUT Office 102 to officeNumber 101: **409**. Updating Office 101 with its own officeNumber 101: **200**. Database uniqueness also protects officeNumber.
- POST Doctor without `office`, or with `office: null`: **400**. PUT an existing Doctor without office: **400**. A nonexistent office UUID: **404**. Assigning an occupied office to another doctor: **409**.
- POST Appointment with a missing date, status, patient.id or doctor.id: **400**. Nonexistent patient/doctor UUID: **404**. An invalid enum string: **400**.
- POST a duplicate specialization name (case insensitive): **409**.
- DELETE an Office still assigned to a Doctor: **409**. DELETE a Doctor or Patient with Appointments: **409**.

Error-body formatting is Spring Boot's default; check the HTTP status rather than expecting a particular error JSON message.

### Delete order (last)

Delete all ten Appointments first, then the three Doctors (their specialization links are removed), then Patients, Offices and Specializations. Deleting a Specialization removes its join-table links and retains Doctors. Do not delete seed records before the live quiz. For non-destructive CRUD trials, create extra temporary records and delete those.

## 7. PostgreSQL and SQL logging

The existing PostgreSQL URL and credentials are preserved in `src/main/resources/application.properties`. Ensure PostgreSQL is running on localhost:5432, database `clinic` exists, and the configured local credentials work. The application uses `ddl-auto=update`. Hibernate prints SQL with `spring.jpa.show-sql=true` and formats it with `spring.jpa.properties.hibernate.format_sql=true`.

Before starting against old Exercise 4 data, inspect it locally: existing null doctor.office_id values must be assigned valid, distinct Offices; old status values outside the four quiz values must be corrected; duplicate office numbers must be resolved. Hibernate schema update cannot clean existing data, and an existing PostgreSQL enum/check constraint may need to be aligned with the four values. No existing database rows have been changed by this task.

## 8. Build and verification

Run `mvn clean test` to compile the baseline. There are currently no automated test sources. Perform the HTTP/constraint checks above against your local PostgreSQL database and keep the terminal visible for the SQL demo.

This baseline removes old Exercise 4 DTO/query endpoints. Part A is implemented below; Parts B, C and Bonus remain deferred.

## PART A — DERIVED QUERIES

Use the existing Postman environment variables from the seed sections. All GET requests below have **no body**. All repository queries use Spring Data method names; none uses `@Query`.

### A1 — Patients by last name

Send both requests:

- **GET http://localhost:8080/api/patients/by-last-name?lastName=uwase**
- **GET http://localhost:8080/api/patients/by-last-name?lastName=UWASE**

Expected for each: **200 OK**, the same Patient array with **Diane Uwase first, Grace Uwase second**, ordered by firstName ascending. Each object contains id, firstName, lastName and dateOfBirth. A request with `lastName=NoSuchPatient` returns **200** and `[]`.

Repository: `findByLastNameIgnoreCaseOrderByFirstNameAsc(String lastName)`.
Service: `getPatientsByLastName(String lastName)`.
Both `/api/patient` and `/api/patients` expose Patient routes, preserving old CRUD URLs.

### A2 — Appointments by status

**GET http://localhost:8080/api/appointments/by-status?status=SCHEDULED**

Expected with the unmodified seed: **200 OK**, Appointments **1, 6, 10** ordered by appointmentDate: `2026-10-05`, `2026-11-02`, `2026-11-27`. Each entity includes its nested doctor and patient. Perform this before the earlier CRUD PUT example, which changes Appointment 1's status.

Also test `status=CONFIRMED`, `status=COMPLETED` and `status=CANCELLED`; each returns only that status, earliest date first. Invalid enum values return **400**. A valid status with no matching rows returns **200** and `[]`.

Repository: `findByStatusOrderByAppointmentDateAsc(AppointmentStatus status)`.
Service: `getByStatus(AppointmentStatus status)`.

### A3 — Inclusive appointment date range

**GET http://localhost:8080/api/appointments/between?start=2026-10-01&end=2026-10-31**

Expected with either the original foundation seed or the fresh-seed alternative above: **200 OK**, Appointments **1–5**, sorted by appointmentDate ascending. No November appointments appear. Equal-date rows have no guaranteed order relative to one another.

To verify boundaries using existing rows:

- **GET http://localhost:8080/api/appointments/between?start=2026-10-05&end=2026-10-25** includes appointments on both October 5 and October 25.
- **GET http://localhost:8080/api/appointments/between?start=2026-10-25&end=2026-10-25** returns Appointment 5, showing that the same start/end date is included.
- **GET http://localhost:8080/api/appointments/between?start=2027-01-01&end=2027-01-31** returns **200** and `[]` if no additional records were created for that period.

Send dates as `yyyy-MM-dd`. The controller receives two Strings and calls `LocalDate.parse` on each, without try/catch. The service converts the parsed values to `java.sql.Date` to match Appointment.appointmentDate. `Between` includes both boundaries.

Repository: `findByAppointmentDateBetweenOrderByAppointmentDateAsc(Date start, Date end)`.
Service: `getBetweenDates(LocalDate start, LocalDate end)`.

### A4 — Prevent Doctor double booking on save

The existing **POST http://localhost:8080/api/appointments/save** endpoint performs the check in the service after validating and resolving doctor/patient references. Use raw JSON and `Content-Type: application/json`.

Repository: `existsByDoctorIdAndAppointmentDateAndStatusNot(UUID doctorId, Date appointmentDate, AppointmentStatus status)`, called with `AppointmentStatus.CANCELLED`.

#### TEST 1 — Conflict

Doctor 1 already has a SCHEDULED appointment on October 5. Submit:

```json
{
  "appointmentDate": "2026-10-05",
  "reason": "Double booking test",
  "status": "CONFIRMED",
  "patient": { "id": "{{patient2}}" },
  "doctor": { "id": "{{doctor1}}" }
}
```

Expected: **409 CONFLICT**, with the exact plain text response body:

```text
Doctor is already booked on that date
```

No appointment is saved. Check **GET http://localhost:8080/api/appointments/all** before and after; the count must stay the same.

Repeat with Doctor 3 on `2026-10-25` (existing CONFIRMED) and Doctor 1 on `2026-10-12` (existing COMPLETED): both must return the same **409** and exact text. The check looks at the existing appointment's status, so even a new CANCELLED request conflicts if an existing non-CANCELLED appointment books that doctor/date.

#### TEST 2 — Allowed when the only existing appointment is CANCELLED

Doctor 2's October 18 appointment is CANCELLED. Submit:

```json
{
  "appointmentDate": "2026-10-18",
  "reason": "Replacement for cancelled appointment",
  "status": "SCHEDULED",
  "patient": { "id": "{{patient5}}" },
  "doctor": { "id": "{{doctor2}}" }
}
```

Expected on the first submission: **201 CREATED**, the saved Appointment entity with generated id, date, reason, status, and resolved patient/doctor references. Capture the id as `partAReplacement`. There should now be two Doctor 2 appointments on that date: the old CANCELLED row and the new SCHEDULED row.

Repeat this request: expected **409** with the exact conflict text, because the new SCHEDULED row now books that date. To restore the original seed, send **DELETE http://localhost:8080/api/appointments/delete/{{partAReplacement}}**, with no body; expect **200** and `Appointment deleted successfully`.

An unbooked date is also allowed: repeat the body with `appointmentDate` set to `2026-11-30`, assuming Doctor 2 has no non-CANCELLED booking on that date. Expect **201** and delete the temporary record afterwards.

A4 modifies only POST save. Existing appointment PUT behavior is preserved. Doctor's required Office and unique Office number constraints remain unchanged.

### Part A validation scope

Run `mvn clean test`. The repository currently has no automated test sources. The HTTP results above are manual PostgreSQL/Postman checks to perform locally; compilation alone does not prove database-backed query results.
