# Sunrise Dental Clinic — Appointment & Patient Management System

CIS6003 Advanced Programming — WRIT1
Cardiff Metropolitan University / ICBT Campus

**Student:** [Your Name]  **ID:** [stXXXXXXXX]

---

## Overview

A Java Swing desktop application replacing a paper appointment diary that
suffered from double bookings, lost records, long waits and billing errors.

## Architecture

```
   VIEW  ───────►  CONTROLLER  ───────►  DAO (model)  ───────►  MySQL
  (Swing)          (validation,          (all SQL             (WampServer)
                    decisions,            lives here)
                    threading)
```

**One rule, never broken:** no `view` class contains SQL or imports
`java.sql`. That single constraint is what makes the three-tier claim
defensible when a marker reads the code.

## Technology

| Layer | Technology |
|---|---|
| Interface | Java Swing / AWT, hand-coded (no GUI builder) |
| Logic | Plain Java controllers, SwingWorker for background work |
| Data access | JDBC with PreparedStatement |
| Database | MySQL 8 under WampServer |
| Testing | JUnit 4 |
| Build | Apache Ant (NetBeans) |

## Design patterns

| Pattern | Where | Why |
|---|---|---|
| MVC | `model` / `view` / `controller` packages | Separates interface from logic and data |
| Singleton | `DBConnection`, `Session` | One shared connection; one signed-in user |
| DAO | `UserDAO`, `AppointmentDAO`, `SessionDAO` | All SQL isolated in one layer |
| Observer | Swing `addXxxListener` methods | Controllers subscribe to view events |

## Features

- Sign-in with SHA-256 hashed passwords
- **Database-backed session management** with idle timeout and persistent
  "keep me signed in" tokens
- Dashboard with today's figures and schedule
- Register, **edit** and **delete** appointments
- Double-booking prevention (application check plus a database constraint)
- Live filtering across number, patient, contact and dentist
- Automatic bill calculation, printable receipt, save receipt to file
- Management summary per dentist per day
- Patient reminder generation
- Active session monitor with "sign out everywhere"
- In-application help
- Three background threads: clock, session monitor, and SwingWorker loads

## Setup

1. Start WampServer — the tray icon must be **green**
2. phpMyAdmin → SQL tab → paste and run **`sql/dental_clinic.sql`** (one file,
   creates everything)
3. Open the project in NetBeans
4. Right-click **Libraries → Add JAR/Folder** → `lib/mysql-connector-j-8.x.x.jar`
5. Right-click **Libraries → Add Library → JUnit**
6. Run `Main.java`

**Sign in:** `admin` / `admin123`  or  `nimali` / `nimali123`

## Session management

A Swing application has no browser, so there is no `JSESSIONID` cookie. The
same capability is implemented explicitly:

| Web application | This application |
|---|---|
| `JSESSIONID` cookie | random token in `Session` and the local token file |
| server session store | `sessions` table in MySQL |
| session timeout | `expires_at` plus the idle monitor thread |
| `session.invalidate()` | `SessionDAO.end()` |
| persistent cookie | token file at `~/.sunrise-dental/session.token` |

Tokens use `SecureRandom` (256 bits) and are stored **hashed**, so reading the
`sessions` table yields nothing replayable.

## Tests

```
ant test
```

22 JUnit tests across `ValidatorTest`, `AppointmentTest`, `BillTest`,
`ReminderServiceTest` and `SessionTest`.

## Known limitations

- The local token file is plain text; a production system would encrypt it
  using the OS credential store (DPAPI on Windows)
- Patient details are stored on the appointment row rather than a separate
  `patients` table, so a returning patient's details are duplicated
- DAO classes have no automated coverage; they are covered by manual
  integration tests because they need a live database
- Passwords use SHA-256 without a salt; bcrypt or PBKDF2 would be correct
  for production
