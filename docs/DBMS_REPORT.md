# SwipeHire — Database Design Report

MySQL 8, InnoDB, utf8mb4. The schema is hand-written in
[`swipehire-web/src/main/resources/db/schema.sql`](../swipehire-web/src/main/resources/db/schema.sql)
(28 tables + 1 view). Hibernate only *validates* that the entities match it (`ddl-auto=validate`).

## 1. Entities and relationships

| Relationship | Cardinality | Implemented as |
|---|---|---|
| users — applicant_profile | 1 : 0..1 | shared primary key `applicant_profile.user_id` (PK + FK) |
| users — hirer | 1 : 0..1 | shared primary key `hirer.user_id` (PK + FK) |
| company — hirer | 1 : N | `hirer.company_id` FK |
| company — job | 1 : N | `job.company_id` FK |
| hirer — job (posted by) | 1 : N | `job.posted_by` FK → `hirer.user_id` |
| college — applicant_profile | 1 : N (optional) | `applicant_profile.college_id` FK, set only after OTP verification |
| applicant — skill | M : N **with attribute** proficiency | junction `applicant_skill(applicant_id, skill_id, proficiency)` |
| job — skill | M : N **with attributes** weight, is_required | junction `job_skill(job_id, skill_id, weight, is_required)` |
| job — college (allowed) | M : N | junction `job_allowed_college` |
| resume_item — skill | M : N | junction `resume_item_skill` |
| applicant — preferred city | 1 : N (multi-valued attribute) | `applicant_pref_city(applicant_id, city)` |
| applicant — job (applicant swipe) | M : N, at most one per pair | `applicant_swipe` + `UNIQUE(applicant_id, job_id)` |
| applicant — job (hirer swipe) | M : N, at most one per pair | `hirer_swipe` + `UNIQUE(applicant_id, job_id)` |
| applicant — job (match) | M : N, at most one per pair | `job_match` + `UNIQUE(applicant_id, job_id)` |
| job_match — application | 1 : 1 | `application.match_id` FK + `UNIQUE` |
| application — status history | 1 : N | `application_status_history.application_id` FK |
| application — interview_slot | 1 : N | `interview_slot.application_id` FK |
| application — resume_item (tailored) | M : N with rank and relevance | `tailored_resume_item` |
| job_match — message | 1 : N | `message.match_id` FK |
| users — notification | 1 : N | `notification.user_id` FK |
| applicant — job (score) | M : N with 6 score attributes | `match_score_cache` |

`job_match` is the name of the match table because `MATCH` is a reserved word in MySQL.

## 2. Normalisation

**1NF (atomic values, no repeating groups).** The proposal deck had `skills[]` and `skills_required[]`
array columns; these became junction tables (`applicant_skill`, `job_skill`). Preferred cities, allowed
colleges and resume-item skills are also separate tables. No column stores a list, JSON or CSV.

**2NF (no partial dependency on part of a composite key).** Every table with a composite key has
non-key columns that depend on the *whole* key:
- `applicant_skill(applicant_id, skill_id) → proficiency`: proficiency is about that applicant *and* that skill.
- `job_skill(job_id, skill_id) → weight, is_required`.
- `daily_quota(applicant_id, quota_date) → right_used, super_used`.
- `match_score_cache(applicant_id, job_id) → total, skill_coverage, ...`.
Skill names live only in `skill`, not repeated in the junction tables.

**3NF (no transitive dependency).**
- A hirer's company name is not copied into `job`; it is reached through `job.company_id → company.name`.
- `job.company_id` is *not* derivable from `job.posted_by → hirer.company_id`: a job belongs to the company
  even if its recruiter later moves to another company. So both columns are facts about the job itself.
- `application.status` is the *current* state; the full history is in `application_status_history`
  (no "status_1, status_2..." columns).
- Matched/missing skill lists are **not** stored in `match_score_cache`; they are derived from
  `applicant_skill` and `job_skill`, so they can never get out of sync.
- The college of a student is a foreign key; the college name and domain are stored once in `college`.

**Deliberate, documented redundancy.** `match_score_cache` stores computed scores. It is a cache
(it can be rebuilt any time from the source tables) kept so a score can be explained later and analysed in SQL.

## 3. Integrity constraints

| Kind | Examples |
|---|---|
| Primary keys | surrogate `BIGINT AUTO_INCREMENT` ids; composite keys on junction tables; shared PK for 1:1 |
| Foreign keys | every reference, with `ON DELETE CASCADE` for owned rows (e.g. swipes of a deleted user) and `ON DELETE SET NULL` for audit references (`changed_by`, `verified_by`) |
| Unique | `users.email`, `college.email_domain`, `company.name`, `applicant_profile.college_email`, `(applicant_id, job_id)` on both swipe tables and on `job_match`, `application.match_id` |
| CHECK (enforced since MySQL 8.0.16) | enum-like columns (`role`, `status`, `direction`, `work_mode`...), `proficiency BETWEEN 1 AND 5`, `weight BETWEEN 1 AND 5`, `right_used BETWEEN 0 AND 25`, `super_used BETWEEN 0 AND 3`, `stipend_max >= stipend_min`, `end_date >= start_date`, `direction = 'SUPER' OR pitch IS NULL`, `work_mode = 'REMOTE' OR city IS NOT NULL`, OTP `attempts BETWEEN 0 AND 5` |

Business rules enforced **by the database itself**, not only by Java: one swipe per pair, one match per
pair, quota upper bounds, a pitch only on super-swipes, a city for non-remote jobs.

## 4. Indexes

| Index | Serves |
|---|---|
| `job(status, deadline)` | loading the deck: active jobs whose deadline hasn't passed |
| `applicant_swipe(job_id, direction)` | hirer queue: who liked this job |
| `hirer_swipe(job_id)`, `job_match(job_id)` | funnel and pipeline per job |
| `application(status, status_changed_at)` | hourly expiry job: stale MATCHED/APPLIED applications |
| `application_status_history(application_id, changed_at)` | timeline of one application |
| `notification(user_id, read_at)` | unread count in the header on every page |
| `message(match_id, sent_at)` | chat thread in order |
| `match_score_cache(job_id, total)` | ranking candidates per job |
| `email_verification(user_id, created_at)` | latest OTP of a user |

## 5. View

`v_hirer_responsiveness`: for each hirer, of the applications that were applied to and are "decided"
(the hirer acted, or 7 days passed), how many the hirer acted on within 7 days of the applicant applying.
It uses a correlated `EXISTS` subquery inside `SUM(CASE ...)` with `GROUP BY`. The % is shown on every job card.

## 6. Transactions and concurrency

| Problem | Solution | Code |
|---|---|---|
| **Lost update on quota**: two requests read `right_used = 24` and both write 25, so 26 swipes happen | single atomic statement `UPDATE daily_quota SET right_used = right_used + 1 WHERE ... AND right_used < 25`; 0 rows affected = limit reached | `QuotaDao.tryConsume` |
| **Missed match**: applicant and hirer like each other at the same instant; each transaction checks for the other's swipe before the other has committed, so no match is created | both kinds of swipe transaction first lock the applicant's row (`SELECT ... FOR UPDATE`), so they run one after another; the second reads the first's swipe with `FOR SHARE` (latest committed version) | `SwipeDao.lockApplicant`, `findHirerDirection`, `findApplicantDirection` |
| **Duplicate match** | `INSERT IGNORE` + `UNIQUE(applicant_id, job_id)` | `SwipeDao.insertMatchIfAbsent` |
| **Partial work** (quota used but swipe rejected) | the whole swipe is one transaction; any error rolls back the quota increment too | `SwipeService.applicantSwipe` |
| **Side effects of a rolled-back match** | notifications/email/PDF run only `AFTER_COMMIT` | `MatchEventListener` |
| **Brute-forcing an OTP** | the failed-attempt counter is committed even though the request fails (`noRollbackFor`) | `VerificationService.confirmOtp` |

JDBC and Hibernate share the same connection and transaction (`DataSourceUtils`), so a swipe written
with a raw `PreparedStatement` and the `Application` saved by Hibernate commit or roll back together.
These guarantees are tested under real concurrency in `QuotaAndMatchTest` (40 threads on the quota,
simultaneous mutual likes produce exactly one match).

## 7. Main queries (where to find them)

| Query | Technique | Location |
|---|---|---|
| Deck candidates: active, not expired, not yet swiped | `NOT EXISTS` subquery | `ScoringDataDao.loadCandidateJobs` |
| Hirer queue: liked the job, not reviewed yet, super-likes first | `NOT EXISTS`, `ORDER BY (direction = 'SUPER') DESC` | `SwipeDao.queueCandidates` |
| Discover: discoverable applicants with no interaction on this job | two `NOT EXISTS` | `ScoringDataDao.discoverCandidateIds` |
| Hiring funnel | 7 scalar subqueries in one `SELECT` | `AnalyticsDao.jobFunnel` |
| Skill gap of the applicant pool | `LEFT JOIN` + `COUNT` / `NULLIF` + `GROUP BY` | `AnalyticsDao.skillGap` |
| Average hirer response time | derived table with `MIN(changed_at)` per application, `TIMESTAMPDIFF`, `AVG` | `AnalyticsDao.avgResponseHours` |
| Top missing skills for an applicant | join + `NOT EXISTS` + `GROUP BY ... ORDER BY COUNT(*) DESC LIMIT` | `AnalyticsDao.applicantMissingSkills` |
| Daily activity | `UNION ALL` of three grouped queries, regrouped | `AnalyticsDao.dailyActivity` |
| Pipeline export | 5-table join with `LEFT JOIN`s | `AnalyticsDao.pipelineRows` |
| Applications with job and company in one query | HQL `join fetch` | `ApplicationRepository.findForApplicant` |
| Admin user search | JPA Criteria API | `AdminService.searchUsers` |

### Example: the funnel for job 1

```sql
SELECT
  (SELECT COUNT(*) FROM match_score_cache WHERE job_id = 1)                                   AS shown,
  (SELECT COUNT(*) FROM applicant_swipe  WHERE job_id = 1 AND direction IN ('RIGHT','SUPER')) AS liked,
  (SELECT COUNT(*) FROM hirer_swipe      WHERE job_id = 1 AND direction = 'RIGHT')            AS hirer_liked,
  (SELECT COUNT(*) FROM job_match        WHERE job_id = 1)                                    AS matched;
```

### Example: why did a card get its score?

```sql
SELECT j.title, ROUND(total*100) AS score, ROUND(skill_coverage*100) AS skills, ROUND(tfidf_cosine*100) AS text,
       ROUND(preference_boost*100) AS taste, ROUND(location_fit*100) AS location, ROUND(stipend_fit*100) AS stipend
FROM match_score_cache s JOIN job j ON j.id = s.job_id
WHERE s.applicant_id = 101
ORDER BY total DESC;
```
