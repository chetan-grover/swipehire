package com.swipehire.web.service;

import com.swipehire.common.dto.JobData;
import com.swipehire.common.tailoring.ResumeItemData;
import com.swipehire.common.tailoring.ResumeTailor;
import com.swipehire.common.tailoring.TailoredResume;
import com.swipehire.web.config.Storage;
import com.swipehire.web.dao.ScoringDataDao;
import com.swipehire.web.dao.TailoredResumeDao;
import com.swipehire.web.entity.ApplicantProfile;
import com.swipehire.web.entity.ApplicantSkill;
import com.swipehire.web.entity.Application;
import com.swipehire.web.entity.Job;
import com.swipehire.web.entity.ResumeItem;
import com.swipehire.web.entity.Skill;
import com.swipehire.web.entity.User;
import com.swipehire.web.repository.ApplicationRepository;
import com.swipehire.web.repository.ResumeItemRepository;
import com.swipehire.web.support.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Generates the job-tailored resume PDF for an application.
 * <p>
 * Runs on a background thread right after a match (generateAsync), and on demand if someone
 * opens the PDF before it is ready (ensurePdf). A per-application lock object makes sure two
 * threads never write the same file at the same time.
 */
@Service
public class TailoringService {

    private static final Logger log = LoggerFactory.getLogger(TailoringService.class);

    private final Map<Long, Object> locks = new ConcurrentHashMap<>();
    private final ApplicationRepository applications;
    private final ResumeItemRepository resumeItems;
    private final ScoringDataDao scoringDao;
    private final TailoredResumeDao tailoredDao;
    private final SkillCatalog catalog;
    private final ResumePdfWriter pdfWriter;
    private final Storage storage;
    private final ExecutorService taskPool;
    private final TransactionTemplate tx;

    public TailoringService(ApplicationRepository applications, ResumeItemRepository resumeItems,
                            ScoringDataDao scoringDao, TailoredResumeDao tailoredDao, SkillCatalog catalog,
                            ResumePdfWriter pdfWriter, Storage storage, ExecutorService taskPool,
                            PlatformTransactionManager txManager) {
        this.applications = applications;
        this.resumeItems = resumeItems;
        this.scoringDao = scoringDao;
        this.tailoredDao = tailoredDao;
        this.catalog = catalog;
        this.pdfWriter = pdfWriter;
        this.storage = storage;
        this.taskPool = taskPool;
        // Background threads have no request transaction, so we open one explicitly.
        this.tx = new TransactionTemplate(txManager);
    }

    public CompletableFuture<Path> generateAsync(long applicationId) {
        return CompletableFuture.supplyAsync(() -> generate(applicationId), taskPool)
                .whenComplete((path, err) -> {
                    if (err != null) {
                        log.error("Tailored PDF for application {} failed", applicationId, err);
                    } else {
                        log.info("Tailored PDF for application {} written to {} on {}", applicationId, path,
                                Thread.currentThread().getName());
                    }
                });
    }

    /** Returns the PDF, generating it first if it doesn't exist yet. */
    public Path ensurePdf(long applicationId) {
        String existing = tx.execute(s -> applications.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found")).getTailoredPdfPath());
        if (existing != null && Files.exists(Path.of(existing))) {
            return Path.of(existing);
        }
        return generate(applicationId);
    }

    /** Regenerates (e.g. after the applicant updated their resume items). */
    public Path generate(long applicationId) {
        synchronized (locks.computeIfAbsent(applicationId, id -> new Object())) {
            return tx.execute(status -> doGenerate(applicationId));
        }
    }

    private Path doGenerate(long applicationId) {
        Application app = applications.findById(applicationId).orElseThrow(() -> new NotFoundException("Application not found"));
        ApplicantProfile profile = app.getMatch().getApplicant();
        User user = profile.getUser();
        Job job = app.getMatch().getJob();
        JobData jobData = scoringDao.loadJob(job.getId()).orElseThrow();

        List<ResumeItemData> items = new ArrayList<>();
        for (ResumeItem ri : resumeItems.findByApplicantIdOrderByDisplayOrderAsc(profile.getUserId())) {
            items.add(new ResumeItemData(ri.getId(), ri.getItemType(), ri.getTitle(), ri.getOrganization(),
                    ri.getDateRange(), ri.getDescription(),
                    ri.getSkills().stream().map(Skill::getId).collect(Collectors.toSet()), ri.getDisplayOrder()));
        }
        List<Long> skillIds = profile.getSkills().stream().map(ApplicantSkill::getSkill).map(Skill::getId).toList();

        TailoredResume tailored = new ResumeTailor().tailor(items, skillIds, jobData);

        String contact = String.join("  ·  ", List.of(user.getEmail(),
                        Objects.toString(profile.getPhone(), ""), Objects.toString(profile.getCity(), ""))
                .stream().filter(x -> !x.isBlank()).toList());
        String college = profile.isCollegeVerified()
                ? "Verified student · " + profile.getCollege().getName()
                + (profile.getGradYear() != null ? " · Class of " + profile.getGradYear() : "")
                : (profile.getDegree() != null ? profile.getDegree() : null);
        Path file = storage.generatedPdfs().resolve("application-" + applicationId + ".pdf");
        try {
            pdfWriter.write(file, new ResumePdfWriter.Header(user.getFullName(), contact, profile.getHeadline(), college),
                    tailored, catalog.names(tailored.orderedSkillIds()),
                    catalog.names(List.copyOf(tailored.highlightSkillIds())), job.getTitle(), job.getCompany().getName());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        tailoredDao.replace(applicationId, tailored.allItems());
        app.setTailoredPdfPath(file.toString());
        return file;
    }

    /** The chosen items in order, for showing "why these items" on the match page. */
    public List<TailoredResume.ScoredItem> preview(long applicationId) {
        return tx.execute(status -> {
            Application app = applications.findById(applicationId).orElseThrow();
            ApplicantProfile profile = app.getMatch().getApplicant();
            JobData jobData = scoringDao.loadJob(app.getMatch().getJob().getId()).orElseThrow();
            List<ResumeItemData> items = resumeItems.findByApplicantIdOrderByDisplayOrderAsc(profile.getUserId()).stream()
                    .map(ri -> new ResumeItemData(ri.getId(), ri.getItemType(), ri.getTitle(), ri.getOrganization(),
                            ri.getDateRange(), ri.getDescription(),
                            ri.getSkills().stream().map(Skill::getId).collect(Collectors.toSet()), ri.getDisplayOrder()))
                    .toList();
            return new ResumeTailor().tailor(items, List.of(), jobData).allItems();
        });
    }
}
