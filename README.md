# Sunrise Dental Clinic — Appointment & Patient Management System

CIS6003 Advanced Programming — WRIT1
Cardiff Metropolitan University / ICBT Campus

**Student:** Tharindu Hulangamuwa  **ID:** [st20288713]

Built to **Solution Type 01**: JFrame application → HTTP/REST → Java web
service → JDBC → WAMP.

---

## Architecture

Two separate JVM processes:

```
  PROCESS 1  (any reception PC)          PROCESS 2  (clinic server)

  ┌──────────────────────────┐           ┌────────────────────────────┐
  │  view      Swing screens │           │  ApiServer   port 8081     │
  │  controller  decisions   │  HTTP     │  requireAuth on every route│  JDBC
  │  ApiClient   HttpURLConn │ ────────▶ │  DAOs        all the SQL   │ ────▶ MySQL
  └──────────────────────────┘  JSON     └────────────────────────────┘       (WAMP)
       no JDBC, no credentials                validates independently
```

**The client contains zero JDBC.** Verified by inspection: 0 `java.sql`
imports in the `view` and `controller` packages, and no `DBConnection`
reference anywhere in the client half.

## The five Type 01 steps

| Step | Where it is |
|---|---|
| 1. JFrame application | `view` + `controller`, started by `Main` |
| 2. HTTP / REST | `ApiClient` ⇄ `ApiServer`, JSON over HTTP |
| 3. Java web service | `ApiServer`, `com.sun.net.httpserver`, port 8081 |
| 4. JDBC | `UserDAO`, `AppointmentDAO`, `SessionDAO`, all `PreparedStatement` |
| 5. WAMP | MySQL 8, schema in `sql/dental_clinic.sql` |

## Design patterns

| Pattern | Where | Why |
|---|---|---|
| MVC | `model` / `view` / `controller` | Separates interface, logic and data |
| Singleton | `DBConnection`, `Session` | One shared connection; one signed-in user |
| DAO | the three `*DAO` classes | All SQL isolated behind plain Java methods |
| Observer | Swing `addXxxListener` | Controllers subscribe to view events |
| Facade | `ApiClient` | One simple Java API hiding all HTTP detail |

## Setup

1. **Start WampServer** — tray icon must be **green**
2. **phpMyAdmin → SQL tab** → run all of `sql/dental_clinic.sql`
   (one file; drops and recreates everything, safe to re-run)
3. Open the project in NetBeans
4. **Libraries → Add JAR/Folder** → `lib/mysql-connector-j-8.x.x.jar`
5. **Libraries → Add Library → JUnit**
6. **Start the server first:** right-click `ApiServer.java` → **Run File**
7. **Check it in a browser:** `http://localhost:8081/api/health`
8. **Then the client:** right-click `Main.java` → **Run File**

See `HOW-TO-RUN.txt` for the full step-by-step with troubleshooting.

Sign in: `admin` / `admin123` (ADMIN) or `nimali` / `nimali123` (STAFF)

> Running the client without the server gives
> "Cannot reach the clinic server. Is ApiServer running?" — that message is
> the two-process design working as intended.

## REST API

Every route except `/api/login` needs `Authorization: Bearer <token>`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/login` | Sign in, returns the session token |
| POST | `/api/logout` | Close the session |
| GET | `/api/health` | **Service status — the only route needing no token** |
| GET | `/api/session/me` | Who the token belongs to |
| GET | `/api/appointments` | All appointments |
| GET | `/api/appointments/{no}` | One appointment |
| GET | `/api/appointments/search?q=` | Free-text search |
| GET | `/api/appointments/date?on=` | One day's list |
| GET | `/api/appointments/check?dentist=&date=&time=` | Slot availability |
| GET | `/api/appointments/next` | Next free number |
| POST | `/api/appointments` | Create |
| PUT | `/api/appointments/{no}` | Update |
| DELETE | `/api/appointments/{no}` | Delete |
| GET | `/api/treatments` | Price list |
| GET | `/api/dashboard` | Headline figures |
| GET | `/api/reports/daily` | Per-dentist summary |
| POST | `/api/reminders` | Generate patient reminders |
| GET | `/api/sessions` | Active sessions |
| DELETE | `/api/sessions` | Sign out everywhere |
| GET | `/api/users` | Staff accounts (**ADMIN only**) |
| POST | `/api/users` | Create account (**ADMIN only**) |
| PUT | `/api/users/{id}` | Reset password / change role (**ADMIN only**) |
| DELETE | `/api/users/{id}` | Delete account (**ADMIN only**) |

Status codes: `200` OK, `201` Created, `400` Bad Request, `401` Unauthorized,
`403` Forbidden, `404` Not Found, `409` Conflict, `500` Server Error.

## Roles

| | STAFF | ADMIN |
|---|---|---|
| Appointments, billing, reports, reminders | yes | yes |
| Staff Accounts | **no** | yes |

Enforced on the **server** (`403 Forbidden`), with the client also hiding the
screen. Hiding a button is a convenience; only the server check is a control,
because a hand-written HTTP request never sees the button.

## Session management

A Swing client has no browser, so there is no `JSESSIONID` cookie. The same
capability is built explicitly:

| Web application | This application |
|---|---|
| `JSESSIONID` cookie | token in `Authorization: Bearer` header |
| server session store | `sessions` table in MySQL |
| session timeout | `expires_at` + the session monitor thread |
| `session.invalidate()` | `POST /api/logout` |
| persistent cookie | token file at `~/.sunrise-dental/session.token` |

Tokens use `SecureRandom` (256 bits) and are stored **hashed**, so reading
the `sessions` table yields nothing replayable.

## Threading

| Thread | Type | Purpose |
|---|---|---|
| clock | daemon `Thread` | Status bar clock |
| session monitor | daemon `Thread` | Idle detection and automatic sign-out |
| workers | `SwingWorker` | Every HTTP call, off the Event Dispatch Thread |
| server pool | fixed pool of 10 | Concurrent request handling |

A network call is slower than a local query, so moving to a web service makes
the `SwingWorker` more necessary, not less.

## Tests

```
ant test
```

**296 JUnit test cases** across 12 classes:

| Class | Tests |
|---|---|
| `ValidatorTest` | 90 |
| `JsonUtilTest` | 43 |
| `ApiServerValidationTest` | 26 |
| `SessionTest` | 23 |
| `AppointmentTest` | 21 |
| `BillTest` | 21 |
| `ReminderServiceTest` | 18 |
| `PasswordUtilTest` | 12 |
| `UserTest` | 11 |
| `SessionStoreTest` | 11 |
| `ApiExceptionTest` | 11 |
| `ApiClientTest` | 9 |

## Known limitations

- HTTP rather than HTTPS; a clinic LAN deployment, not internet-facing
- The local token file is plain text; production would encrypt it with the OS
  credential store (DPAPI on Windows)
- One shared JDBC connection rather than a pool
- Patient details live on the appointment row rather than a `patients` table,
  so a returning patient's details are duplicated
- No rate limiting on sign-in attempts
- DAO classes have no automated coverage; they need a live database and are
  covered by manual integration tests
