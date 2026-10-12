# Syllabus Map

Each syllabus topic, and the exact file that demonstrates it. Paths are relative to the repository root.
`web/` = `swipehire-web/src/main/java/com/swipehire/web/`, `common/` = `swipehire-common/src/main/java/com/swipehire/common/`.

## 1. Servlets

| What | Where |
|---|---|
| `@WebServlet` registered next to Spring MVC via `@ServletComponentScan` | `web/SwipeHireApplication.java` |
| Binary download: content type, `Content-Disposition`, streaming bytes to `getOutputStream()` | `web/servlet/ResumePdfServlet.java` |
| File upload with `@MultipartConfig`, `request.getPart()`, size limits, safe file names | `web/servlet/PhotoServlet.java`, `web/servlet/ResumeImportServlet.java` |
| One servlet mapped to several URL patterns; `getServletPath()`; writing CSV with `PrintWriter` | `web/servlet/ExportAndChartServlet.java` |
| Getting Spring beans inside a servlet (`WebApplicationContextUtils`); `request.getUserPrincipal()` | `web/servlet/BaseServlet.java` |
| `HttpSession` attributes shared between a servlet and a controller | `ResumeImportServlet` → `ApplicantProfileController.importReview` |

## 2. JSP / JSTL

| What | Where |
|---|---|
| Static includes (`<%@ include %>`) for a shared layout | `swipehire-web/src/main/webapp/WEB-INF/jsp/common/header.jspf`, `footer.jspf`, `chat.jspf` |
| JSTL core: `c:forEach`, `c:choose/when/otherwise`, `c:if`, `c:set`, `c:out` (HTML escaping), `c:remove` | every page under `WEB-INF/jsp/` (e.g. `applicant/deck.jsp`) |
| JSTL functions: `fn:length`, `fn:startsWith` | `hirer/dashboard.jsp`, `admin/dashboard.jsp` |
| **Custom tag files** with typed attributes | `WEB-INF/tags/scoreBars.tag`, `skillChips.tag`, `csrf.tag` |
| **Custom EL functions** declared in a TLD | `WEB-INF/swipehire.tld` + `web/support/JspFunctions.java` |
| EL: implicit objects (`param`, `sessionScope`, `pageContext`), map/array access, method calls | `login.jsp`, `header.jspf`, `hirer/dashboard.jsp`, `applicant/match.jsp` |

## 3. JDBC

| What | Where |
|---|---|
| `DataSource` → `Connection` → `PreparedStatement` → `ResultSet`, try-with-resources | `web/dao/PlatformStatsDao.java` |
| Joining Spring transactions from raw JDBC (`DataSourceUtils`) | `web/dao/JdbcDao.java` |
| **Batch updates** (`addBatch` / `executeBatch`), upserts | `web/dao/SwipeDao.java#upsertAffinities`, `web/dao/ScoringDataDao.java#saveScores`, `web/service/DemoDataService.java` |
| Generated keys (`RETURN_GENERATED_KEYS`), also for a whole batch | `web/dao/SwipeDao.java#insertMatchIfAbsent`, `DemoDataService.java` |
| Row locking `SELECT ... FOR UPDATE` / `FOR SHARE`; handling duplicate-key `SQLException` (1062) | `web/dao/SwipeDao.java` |
| Atomic conditional `UPDATE` instead of read-then-write | `web/dao/QuotaDao.java` |
| Report queries: aggregates, `GROUP BY`, correlated subqueries, `UNION ALL`, querying a view | `web/dao/AnalyticsDao.java` |
| `ResultSetMetaData` | `AnalyticsDao.java#pipelineRows` |

## 4. Hibernate / JPA

| What | Where |
|---|---|
| Entities mapped to a hand-written schema, `ddl-auto=validate` | `web/entity/*.java`, `src/main/resources/application.properties` |
| Shared-primary-key 1:1 with `@MapsId` | `web/entity/ApplicantProfile.java`, `Hirer.java` |
| Composite keys with `@EmbeddedId` for junction tables that carry data | `web/entity/ApplicantSkill.java`, `JobSkill.java` |
| `@ManyToMany` + `@JoinTable`, `@ElementCollection` + `@CollectionTable` | `Job.java` (allowed colleges), `ResumeItem.java` (skills), `ApplicantProfile.java` (cities) |
| `@OneToMany(cascade, orphanRemoval)`, lifecycle callback `@PreUpdate` | `Job.java` |
| **HQL** with `join fetch` (avoids N+1), bulk `update`/`delete` with `@Modifying` | `web/repository/ApplicationRepository.java`, `ApplicationStatusHistoryRepository.java`, `MessageRepository.java`, `EmailVerificationRepository.java` |
| **Criteria API** (dynamic WHERE built from optional filters) | `web/service/AdminService.java#searchUsers`, `#searchJobs` |
| Spring Data derived queries | `web/repository/*Repository.java` |

## 5. Spring Framework

| What | Where |
|---|---|
| Spring MVC controllers, `@RequestParam`, `@PathVariable`, `@ModelAttribute` form binding, `RedirectAttributes` flash | `web/controller/*.java` |
| Bean validation (`@Valid`, `@Size`, `@Min`) | `web/form/ProfileForm.java`, `ApplicantProfileController#saveProfile` |
| `@ControllerAdvice`: global exception handling and shared model attributes | `web/support/GlobalExceptionHandler.java`, `web/controller/ViewModelAdvice.java` |
| Dependency injection (constructor injection everywhere), `@Configuration` + `@Bean` | `web/config/AsyncConfig.java`, `WebSocketConfig.java` |
| Declarative transactions: `@Transactional`, `REQUIRES_NEW`, `MANDATORY`, `noRollbackFor`, `readOnly` | `NotificationService`, `MatchService`, `VerificationService#confirmOtp`, `PipelineService` |
| Programmatic transactions (`TransactionTemplate`) on background threads | `web/service/TailoringService.java` |
| Application events + `@TransactionalEventListener(AFTER_COMMIT)` | `web/service/MatchCreatedEvent.java`, `MatchEventListener.java` |
| `TransactionSynchronization.afterCommit` | `NotificationService#notify`, `ChatService#send` |
| Spring Security: form login, BCrypt, role-based URLs, CSRF, custom `UserDetails`, success handler | `web/security/SecurityConfig.java`, `AppUserPrincipal.java`, `AppUserDetailsService.java` |

## 6. Java RMI (networking)

| What | Where |
|---|---|
| Remote interface (`extends Remote`, every method `throws RemoteException`) | `common/rmi/MatchEngine.java` |
| Serializable DTOs passed by value between JVMs | `common/dto/ApplicantData.java`, `JobData.java`, `ScoreBreakdown.java` |
| Server: `UnicastRemoteObject`, `LocateRegistry.createRegistry`, `rebind` | `swipehire-engine/.../MatchEngineImpl.java`, `EngineServer.java` |
| Client: `LocateRegistry.getRegistry`, `lookup`, call timeout, reconnect, **fallback when the server is down** | `web/service/EngineClient.java` |
| RMI round-trip test with a real registry | `swipehire-engine/src/test/.../RmiRoundTripTest.java` |

## 7. Multithreading & concurrency

| What | Where |
|---|---|
| Fixed thread pools via `Executors`, named `ThreadFactory`, orderly shutdown | `web/config/AsyncConfig.java`, `swipehire-engine/.../EngineServer.java` |
| Parallel ranking: `Callable` chunks + `invokeAll` + `Future.get` | `common/scoring/Ranker.java#scoreInParallel` |
| `CompletableFuture.supplyAsync` / `runAsync` / `whenComplete` / `exceptionally` | `DeckService#refreshInBackground` (prefetch), `TailoringService#generateAsync`, `EmailService#sendAsync` |
| `Future.get(timeout)` to bound a remote call | `EngineClient#call` |
| `ScheduledExecutorService.scheduleAtFixedRate` (hourly expiry, daily decay) | `web/service/BackgroundJobs.java` |
| `ConcurrentHashMap`, `ConcurrentHashMap.newKeySet()`, `AtomicLong`, `volatile` | `NotificationHub`, `DeckService`, `EngineClient`, `SkillCatalog` |
| `synchronized` blocks (per-object locks; WebSocket send) | `TailoringService#generate`, `NotificationHub#push` |
| `CountDownLatch` (keep server alive; start gate in tests) | `EngineServer.java`, `QuotaAndMatchTest.java` |
| Database-level concurrency: row locks, atomic updates, unique keys | `SwipeDao.java`, `QuotaDao.java`; proven by `QuotaAndMatchTest` |

## 8. WebSockets

| What | Where |
|---|---|
| `@ServerEndpoint` with `@OnOpen/@OnClose/@OnError` | `web/websocket/NotifyEndpoint.java` |
| Handshake configurator reading the `HttpSession` | `web/websocket/HttpSessionConfigurator.java` |
| Thread-safe session registry and push to a user | `web/websocket/NotificationHub.java` |
| Registering endpoints on embedded Tomcat | `web/config/WebSocketConfig.java` |
| Browser client (vanilla JS, < 100 lines) | `swipehire-web/src/main/webapp/static/js/app.js` |

## 9. Libraries used in pure Java

| Library | Purpose | Where |
|---|---|---|
| Apache Lucene `EnglishAnalyzer` | tokenising, stop words, stemming for TF-IDF | `common/scoring/TextAnalyzer.java`, `TfIdfModel.java` |
| OpenPDF | tailored resume PDF | `web/service/ResumePdfWriter.java` |
| Apache PDFBox | extract text from uploaded resumes | `web/servlet/ResumeImportServlet.java`, `common/tailoring/SkillDetector.java` |
| JFreeChart | server-side PNG charts | `web/service/ChartService.java` |
| Jakarta Mail | email (console in dev) | `web/service/EmailService.java` |

## 10. DBMS

See [DBMS_REPORT.md](DBMS_REPORT.md): schema, normalisation, constraints, indexes, view, transactions and the main queries.

## 11. Testing (JUnit 5)

| What | Where |
|---|---|
| Scorer, hard filters, TF-IDF, preferences, ranking + exploration | `swipehire-common/src/test/java/.../scoring/*Test.java` |
| Tailoring selection and skill detection | `swipehire-common/src/test/java/.../tailoring/TailoringTest.java` |
| Application state machine | `swipehire-common/src/test/java/.../enums/ApplicationStatusTest.java` |
| RMI round trip | `swipehire-engine/src/test/java/.../RmiRoundTripTest.java` |
| Quota under 40 concurrent threads, simultaneous mutual likes → one match, rollback, pipeline | `swipehire-web/src/test/java/.../QuotaAndMatchTest.java` (needs MySQL) |
