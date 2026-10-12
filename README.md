# SwipeHire

Mutual-match hiring for internships and fresher roles, built entirely in Advanced Java.
Applicants swipe on jobs, recruiters swipe on applicants, and a mutual like creates a match that
opens an application with a resume tailored to that exact job.

- **Explainable matching**: `0.45 skills + 0.20 TF-IDF (Lucene) + 0.15 learned taste + 0.10 location + 0.10 stipend`, with the breakdown and matched/missing skills on every card
- **Less noise**: 25 likes and 3 super-likes (with a 140-character pitch) per day
- **One-click tailored apply**: the applicant's own projects and skills are selected, reordered and highlighted per job (OpenPDF); nothing is invented
- **No ghosting**: matches expire after 7 days of inactivity; recruiter responsiveness is shown on job cards
- **Verified badges**: college email OTP for students, admin approval for companies
- **Real time**: WebSocket match alerts and chat; RMI match engine in a separate JVM with an automatic fallback

Docs: [Product spec](docs/PRODUCT_SPEC.md) · [Syllabus map](docs/SYLLABUS_MAP.md) · [DBMS report](docs/DBMS_REPORT.md)

## Tech

Java 21 · Maven multi-module · Spring Boot 3.3 (MVC, Security, Data JPA) · Hibernate · plain JDBC ·
JSP + JSTL · Servlets · Jakarta WebSocket · Java RMI · `java.util.concurrent` · MySQL 8 ·
Lucene · OpenPDF · PDFBox · JFreeChart · Jakarta Mail · JUnit 5.
No JavaScript frameworks (about 90 lines of vanilla JS), no Python, no external AI APIs.

```
swipehire-common   RMI interface, Serializable DTOs, enums, Scorer/Ranker (Lucene TF-IDF), ResumeTailor
swipehire-engine   standalone JVM: RMI registry on :1099, parallel ranking on a thread pool
swipehire-web      Spring Boot WAR: JSP views, controllers, Hibernate entities, JDBC DAOs, servlets, WebSocket
```

## Run it

Prerequisites: JDK 21+, MySQL 8 running on `localhost:3306`, Maven 3.9 (on this machine:
`$HOME\tools\apache-maven-3.9.9\bin\mvn`, shown as `mvn` below).

```powershell
$env:DB_PASSWORD = "<your MySQL root password>"     # never commit it
mvn -DskipTests install                              # build all three modules once

# terminal 1: the match engine (RMI server)
mvn -pl swipehire-engine exec:java

# terminal 2: the web app (same DB_PASSWORD env var)
mvn -pl swipehire-web spring-boot:run
```

Open <http://localhost:8080>. The `swipehire` database, tables and demo data are created on first start.
The web app also works without the engine (ranking then runs in-process; the admin page shows "DEGRADED").

| Role | Email | Password |
|---|---|---|
| Applicant | `aarav.mehta@gmail.com` (also ishita.rao, kabir.singh, ananya.iyer, rohan.das, ... @gmail.com) | `demo1234` |
| Hirer | `priya@zenlabs.in`, `arjun@finedge.in`, `meera@cloudnest.io`, `rohit@pixelforge.in` (unverified company) | `demo1234` |
| Admin | `admin@swipehire.in` | `demo1234` |

Admin → **Generate demo data** adds 100 applicants, 30 jobs and 800 swipes (JDBC batch insert) so queues and charts are full.

Optional environment variables: `DB_USER` (default `root`), `DB_HOST`, `DB_NAME`, `PORT`, `SWIPEHIRE_DATA`
(files folder, default `~/swipehire-data`), `MAIL_ENABLED=true` with `MAIL_HOST/MAIL_PORT/MAIL_USER/MAIL_PASSWORD`
to send real email (otherwise emails and OTPs are printed in the web console and the OTP is shown on the page).

## Five-minute demo

1. **Applicant** (`aarav.mehta@gmail.com`): the Discover deck shows "Java Backend Intern" first with its
   score breakdown and ✓/✗ skill chips. Super-like another job with a pitch. Drag a card or use the buttons; try **Undo**.
2. **Hirer** in a second browser/incognito (`priya@zenlabs.in`): Dashboard → *Queue* for Java Backend Intern →
   Aarav is there with his score → **Like**. Both windows get a live **match** toast (WebSocket).
3. Applicant → My Matches → open the match → **View tailored resume** (projects reordered, matched skills bold) → **Apply now**.
4. Hirer → open the application → Shortlist → Interview → propose slots; applicant picks one; chat both ways live.
5. Stop the engine (Ctrl+C in terminal 1) → the deck still loads; Admin overview shows DEGRADED mode. Start it again.
6. Hirer → Analytics (funnel + skill-gap charts, CSV export). Admin → verify PixelForge, Criteria-API user search, audit trail.
7. Applicant (`kabir.singh@gmail.com`) → Verify → `kabir@dtu.ac.in` → the OTP is shown in demo mode → Verified Student badge.
8. Applicant → Skills → upload any PDF resume → PDFBox suggests skills to confirm.

## Tests

```powershell
mvn test                          # unit tests (scorer, ranker, tailoring, state machine, RMI round trip)
$env:DB_PASSWORD = "..."; mvn test # also runs the MySQL concurrency tests (database "swipehire_test")
```

## Troubleshooting

- **Port 1099 already in use**: another engine is running; stop it or leave it (the web app will use it).
- **Changed `schema.sql`**: the script uses `CREATE TABLE IF NOT EXISTS`, so drop the dev database to rebuild:
  `DROP DATABASE swipehire;` then restart the web app.
- **Access denied for user 'root'**: set `DB_PASSWORD` in the same terminal before starting the web app.
