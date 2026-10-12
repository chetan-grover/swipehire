# SwipeHire — Product Specification (v2, refined)

> Mutual-match hiring for internships and fresher roles. An application only moves forward when
> **both** sides want it. Every match gets a resume tailored to that job, and every ranking can be
> explained line by line.
>
> Built 100% in Advanced Java: Servlets, JSP/JSTL, JDBC, Hibernate, Spring MVC, RMI, multithreading, WebSockets.

---

## 1. Positioning

**Open platform** for students and freshers, with **verified college** and **verified company** badges.

| Problem | SwipeHire's answer |
|---|---|
| Mass-apply noise | Daily quota: 25 right-swipes + 3 super-swipes (with a 140-char pitch) |
| Generic resumes | Per-match tailored PDF: the applicant's own content selected, reordered and highlighted, with nothing invented |
| Recruiter ghosting | Matches expire after 7 days of hirer inaction; a **Responsiveness score** is shown on every job card |
| Black-box ranking | Every card shows its score breakdown, matched skills and missing skills |
| Fake profiles / jobs | College email OTP verification; admin verifies companies |

## 2. Roles

- **APPLICANT**: student or fresher looking for an internship or job.
- **HIRER**: recruiter belonging to a company; posts jobs and reviews candidates.
- **ADMIN**: verifies companies, moderates users and jobs, manages the skill dictionary and colleges.

## 3. Feature list (all in scope)

### 3.1 Accounts & verification
- Signup / login (Spring Security, BCrypt, role-based redirects).
- **College verification**: the applicant enters a college email → a 6-digit OTP is mailed (Jakarta Mail; printed to the console in dev)
  → valid for 10 min, max 5 attempts → the email domain is matched against the `college` table → `college_id` is set and the "Verified Student" badge appears.
  Unknown domain → the applicant can request the college be added (the admin approves it).
- **Company verification**: the hirer creates or joins a company; the admin approves it and the "Verified Company" badge appears.
  Unverified companies can post at most 2 active jobs.

### 3.2 Applicant profile
- Basics: name, photo (raw servlet upload), headline, graduation year, degree, city, about.
- Preferences: preferred cities, work mode (REMOTE / HYBRID / ONSITE / ANY), minimum stipend, job type (INTERNSHIP / FULL_TIME), discoverable on/off.
- Skills picked from the dictionary, with a proficiency of 1–5.
- **Resume items**: rows of type PROJECT / EXPERIENCE / ACHIEVEMENT / CERTIFICATION / EDUCATION, each with title, org, dates,
  bullet text and tagged skills. These are the building blocks for tailoring.
- **PDF resume import (Apache PDFBox)**: upload an existing PDF → text is extracted → skills are found via dictionary + synonym match
  → shown pre-ticked for the user to confirm. The extracted text is also stored as the TF-IDF corpus.
  The import never auto-creates resume items, because that would be unreliable and could invent claims.

### 3.3 Applicant swipe deck
- Cards are ranked by the engine (section 4). Each card shows: company + badge, title, stipend, location/mode, deadline,
  hirer responsiveness %, score %, ✅ matched skills, ❌ missing skills.
- Actions: LEFT (pass), RIGHT (like), SUPER (like + 140-char pitch, pinned to the top of the hirer's queue).
- Drag gesture (vanilla JS) **and** plain form POST buttons.
- Quota: 25 RIGHT and 3 SUPER per calendar day (Asia/Kolkata); LEFT is unlimited. The UI shows what is left.
- **Undo last swipe** within 10 seconds (if it has not produced a match yet).
- "A company is interested in you" cards: jobs whose hirer already liked this applicant via Discover are shown with a highlight.

### 3.4 Hirer
- Company profile; **job posting**: title, description (JD), type, skills with weight 1–5 and a required flag, stipend min/max,
  city, mode, openings, application deadline, eligibility (graduation years), "verified students only" filter, allowed colleges (optional).
- Job states: DRAFT → ACTIVE → PAUSED → CLOSED.
- **Per-job queue**: applicants who right-swiped the job, super-swipers first, then by score. The card shows the same breakdown and the pitch.
- **Discover tab**: discoverable applicants who have not swiped the job yet, ranked by score. A hirer right-swipe here is stored,
  and a match forms when the applicant later right-swipes.
- Hirer actions: LEFT / RIGHT.

### 3.5 Match → application pipeline
- A match is created when both an applicant RIGHT/SUPER and a hirer RIGHT exist for the same (applicant, job).
  Detection runs inside the DB transaction of whichever swipe arrives second; `UNIQUE(applicant_id, job_id)` on `job_match` makes it idempotent.
- On match: a WebSocket alert goes to both sides, notifications are inserted, an email is sent (async), and the tailored resume PDF is generated (async).
- **One-click apply**: the applicant reviews the tailored PDF → clicks Apply → status becomes APPLIED.
- Status flow: `MATCHED → APPLIED → SHORTLISTED → INTERVIEW → OFFER | REJECTED`; the applicant can also WITHDRAW.
  Every change is written to `application_status_history` (who, from, to, note, when). Illegal transitions are rejected by a state machine in the service layer.
- **Expiry**: a scheduled job runs every hour; an application untouched for 7 days in MATCHED (the applicant never applied)
  or APPLIED (the hirer never responded) → EXPIRED. A warning goes to whoever has to act when less than 24 hours are left.
- **Responsiveness** (on job cards): of a hirer's applications that are decided (the hirer acted, or 7 days passed since the
  applicant applied), the share the hirer acted on within 7 days of the application. Applicants who never apply don't count against the hirer.
- **Interview scheduling**: at INTERVIEW the hirer proposes up to 3 slots; the applicant picks one; both get a notification.
- **Chat** per match over WebSocket; messages persisted; unread counts.

### 3.6 Tailored resume (no generation, only selection)
1. Score every resume item against the job: weighted overlap of the item's skills with the job's skills + Lucene TF-IDF cosine of item text vs JD.
2. Keep the top N items per section (N configurable); order them by relevance.
3. Skills section: the job's required skills that the applicant has come first, then other matched skills, then the rest.
4. Bold the matched skill names inside bullets.
5. Render with OpenPDF and store the file + a record of which item ids were chosen (`tailored_resume_item`), so it is reproducible and auditable.

### 3.7 Notifications
- Bell icon with unread count; types: MATCH, STATUS_CHANGE, MESSAGE, INTERVIEW, EXPIRY_WARNING (24h before), VERIFICATION.
- Live push over WebSocket, persisted in `notification`.

### 3.8 Analytics (JDBC + JFreeChart PNG)
- **Hirer**: per-job funnel (impressions → likes → matches → applied → offer), skill-gap chart of the applicant pool, average response time.
- **Applicant**: "top missing skills across jobs you liked", swipe/match stats.
- **Admin**: daily signups, swipes, matches; engine health (RMI ping, last ranking latency, degraded-mode count).
- Hirer **CSV export** of a job's pipeline (raw servlet).

### 3.9 Admin
- Approve or reject companies and college requests; ban users; close jobs.
- Skill dictionary CRUD + synonyms ("JS" → JavaScript, "Postgres" → PostgreSQL).
- View the audit trail of status changes.
- Engine health (RMI ping, last ranking mode and latency), run maintenance on demand, generate demo data.

## 4. Matching engine (explainable)

**Hard filters** (a card is never shown if any fails): job ACTIVE and before its deadline; not already swiped; work mode compatible;
graduation year within the job's eligibility; verified-only / allowed-colleges rules; applicant not banned.

**Score** (0–1, each component 0–1, all stored in `match_score_cache`):

```
score = 0.45 · requiredSkillCoverage
      + 0.20 · tfidfCosine
      + 0.15 · preferenceBoost
      + 0.10 · locationModeFit
      + 0.10 · stipendFit
```

| Component | Definition |
|---|---|
| requiredSkillCoverage | Σ(weight × min(1, proficiency/3)) over matched required skills ÷ Σ weight of required skills. A job with no required skills uses all its skills |
| tfidfCosine | Cosine similarity of TF-IDF vectors (Lucene `EnglishAnalyzer`: tokenise, stop-words, stemming) of the applicant corpus vs the JD. IDF is computed over the documents ranked together in one call (all candidate jobs + the applicant, or all candidates + the job) |
| preferenceBoost | Applicant's learned per-skill affinity (`applicant_skill_pref`), averaged over the job's skills, squashed to 0–1 |
| locationModeFit | 1 if REMOTE, or the city is in the preferred cities, or mode = ANY; 0.5 for HYBRID in a nearby/other city; 0 otherwise |
| stipendFit | 1 if stipend_max ≥ desired minimum; linear down to 0 at 50% of the minimum |

**Learning from swipes**: RIGHT → +0.10 per job skill, SUPER → +0.20, LEFT → −0.05; clamped to [−1, 1]; decays ×0.98 daily (scheduled job).
**Cold start / exploration**: every 10th card is drawn at random from the lower half of the remaining ranked jobs and marked "Exploring".
**Hirer side** uses the same pair score, so both sides see the same number and the same explanation.

## 5. Architecture

```
Browser ── JSP/JSTL pages, form POSTs, ≤100 lines vanilla JS (drag + WebSocket)
   │ HTTP                                │ ws://.../ws/notify
   ▼                                     ▼
swipehire-web (Spring Boot 3 WAR, embedded Tomcat)
  ├─ Spring MVC controllers + Spring Security
  ├─ Services (quota, match, pipeline state machine, tailoring, verification)
  ├─ Hibernate/JPA repositories  — entity CRUD, HQL, Criteria API
  ├─ JDBC DAOs (raw PreparedStatement) — swipe writes (batch), analytics, CSV
  ├─ @WebServlet — PDF download, photo upload, resume import, CSV export
  ├─ @ServerEndpoint — notifications + chat
  ├─ Executors — PDF + email (CompletableFuture), hourly expiry, daily preference decay
  └─ RMI client ─────────────► swipehire-engine (separate JVM, registry :1099)
                                 ├─ MatchEngine (remote): rankJobsForApplicant, rankApplicantsForJob, explain
                                 ├─ Scorer (Lucene TF-IDF + rules), parallel ranking via ExecutorService
                                 └─ PreferenceUpdater
                        MySQL 8 (3NF, hand-written schema.sql, ddl-auto=validate)
```

**Fallback**: if the RMI call fails or times out (2s), the web app runs the same `Scorer` (from `swipehire-common`) in-process
and logs "DEGRADED". The demo kills the engine JVM to show the app keeps working.

**Concurrency points (for the viva)**
- Quota: a conditional `UPDATE daily_quota SET right_used = right_used + 1 WHERE ... AND right_used < 25` gives atomic check-and-increment with no lost updates.
- Match creation: a unique key plus `INSERT IGNORE`, so two simultaneous swipes still create exactly one match.
- Ranking: candidate jobs are split into chunks and scored on a fixed thread pool, with results merged by `invokeAll`.
- WebSocket sessions: a `ConcurrentHashMap<userId, Set<Session>>`.

## 6. Data model (3NF) — summary

`users, college, email_verification, applicant_profile, applicant_pref_city, company, hirer, job, skill, skill_synonym,
applicant_skill, job_skill, job_allowed_college, applicant_skill_pref, resume_item, resume_item_skill, resume_upload,
applicant_swipe, hirer_swipe, daily_quota, job_match, application, application_status_history, tailored_resume_item,
interview_slot, message, notification, match_score_cache`

Plus view `v_hirer_responsiveness` and indexes for the deck query. Full DDL: `swipehire-web/src/main/resources/db/schema.sql`.
Design rationale (normalisation, constraints, transactions): `docs/DBMS_REPORT.md`.

## 7. Syllabus coverage

See `docs/SYLLABUS_MAP.md` (kept updated per slice).

## 8. Build plan (vertical slices; build must pass after each)

1. Maven multi-module skeleton, schema.sql, seed data, app boots against MySQL
2. Auth (signup/login/roles), college OTP verification, company + admin verification
3. Applicant profile, skills, resume items, photo upload servlet, PDFBox import
4. Job posting + hirer job management
5. Scorer in `common` + JUnit tests; applicant deck with breakdown
6. Swipes (JDBC batch DAO), quota, undo, match detection + tests
7. RMI engine (separate JVM), parallel ranking, fallback, preference learning
8. Hirer queue + Discover tab
9. Tailored resume (selection + OpenPDF) + tests; one-click apply
10. Pipeline state machine, history, expiry scheduler, interview slots
11. WebSocket notifications + chat
12. Analytics (JDBC + JFreeChart), CSV export, admin panel
13. Polish, seed demo data, SYLLABUS_MAP.md, DBMS report queries

## 9. Five-minute demo script

1. Applicant logs in → the deck shows score breakdowns → super-swipes with a pitch.
2. Hirer (second window) → job queue → the applicant is pinned on top with the pitch → RIGHT.
3. **Both windows get a live match toast.**
4. Open the tailored PDF: items reordered, matched skills bold.
5. Hirer moves to SHORTLISTED → INTERVIEW (slot picked) → show the history table.
6. Kill the engine JVM → the deck still loads (degraded mode) → restart the engine.
7. Hirer analytics charts; admin verifies a company.
