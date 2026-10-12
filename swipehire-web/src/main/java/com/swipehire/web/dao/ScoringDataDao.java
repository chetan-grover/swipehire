package com.swipehire.web.dao;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.WorkMode;
import com.swipehire.web.config.AppTime;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reads exactly the columns the scorer needs and builds the Serializable DTOs that are sent to
 * the RMI engine. Plain JDBC keeps these read-heavy queries fast and fully visible.
 */
@Repository
public class ScoringDataDao extends JdbcDao {

    public ScoringDataDao(DataSource dataSource) {
        super(dataSource);
    }

    // ---------------------------------------------------------------- applicants

    public Optional<ApplicantData> loadApplicant(long applicantId) {
        return withConnection(con -> Optional.ofNullable(loadApplicant(con, applicantId)));
    }

    public List<ApplicantData> loadApplicants(Collection<Long> applicantIds) {
        return withConnection(con -> {
            List<ApplicantData> list = new ArrayList<>();
            for (Long id : applicantIds) {
                ApplicantData a = loadApplicant(con, id);
                if (a != null) {
                    list.add(a);
                }
            }
            return list;
        });
    }

    private ApplicantData loadApplicant(Connection con, long id) throws SQLException {
        ApplicantData a = new ApplicantData();
        StringBuilder text = new StringBuilder();
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT headline, about, degree, grad_year, college_id, college_verified_at,
                       pref_work_mode, pref_job_type, min_stipend
                FROM applicant_profile WHERE user_id = ?""")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                a.setId(id);
                append(text, rs.getString("headline"), rs.getString("about"), rs.getString("degree"));
                int grad = rs.getInt("grad_year");
                a.setGradYear(rs.wasNull() ? null : grad);
                long collegeId = rs.getLong("college_id");
                a.setCollegeId(rs.wasNull() ? null : collegeId);
                a.setCollegeVerified(a.getCollegeId() != null && rs.getTimestamp("college_verified_at") != null);
                a.setPrefWorkMode(WorkMode.valueOf(rs.getString("pref_work_mode")));
                a.setPrefJobType(JobType.valueOf(rs.getString("pref_job_type")));
                a.setMinStipend(rs.getInt("min_stipend"));
            }
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT city FROM applicant_pref_city WHERE applicant_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    a.getPreferredCities().add(rs.getString(1).trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT s.skill_id, s.proficiency, k.name FROM applicant_skill s JOIN skill k ON k.id = s.skill_id WHERE s.applicant_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    a.getSkillProficiency().put(rs.getLong(1), rs.getInt(2));
                    append(text, rs.getString(3));
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT skill_id, affinity FROM applicant_skill_pref WHERE applicant_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    a.getSkillAffinity().put(rs.getLong(1), rs.getDouble(2));
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT title, organization, description FROM resume_item WHERE applicant_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    append(text, rs.getString(1), rs.getString(2), rs.getString(3));
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT extracted_text FROM resume_upload WHERE applicant_id = ? ORDER BY uploaded_at DESC LIMIT 1")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    append(text, rs.getString(1));
                }
            }
        }
        a.setText(text.toString());
        return a;
    }

    /** Discoverable applicants who have not interacted with this job yet (Discover tab). */
    public List<Long> discoverCandidateIds(long jobId, int limit) {
        return withConnection(con -> {
            List<Long> ids = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT p.user_id
                    FROM applicant_profile p
                    JOIN users u ON u.id = p.user_id AND u.enabled = TRUE
                    WHERE p.discoverable = TRUE
                      AND NOT EXISTS (SELECT 1 FROM applicant_swipe s WHERE s.applicant_id = p.user_id AND s.job_id = ?)
                      AND NOT EXISTS (SELECT 1 FROM hirer_swipe h WHERE h.applicant_id = p.user_id AND h.job_id = ?)
                    ORDER BY p.user_id
                    LIMIT ?""")) {
                ps.setLong(1, jobId);
                ps.setLong(2, jobId);
                ps.setInt(3, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getLong(1));
                    }
                }
            }
            return ids;
        });
    }

    // ---------------------------------------------------------------- jobs

    /** Open jobs this applicant has not swiped yet: the raw candidates for the deck. */
    public List<JobData> loadCandidateJobs(long applicantId) {
        return withConnection(con -> loadJobs(con,
                "j.status = 'ACTIVE' AND j.deadline >= ? AND NOT EXISTS "
                        + "(SELECT 1 FROM applicant_swipe s WHERE s.applicant_id = ? AND s.job_id = j.id)",
                Date.valueOf(AppTime.today()), applicantId));
    }

    public Optional<JobData> loadJob(long jobId) {
        return withConnection(con -> loadJobs(con, "j.id = ?", jobId).stream().findFirst());
    }

    private List<JobData> loadJobs(Connection con, String where, Object... params) throws SQLException {
        Map<Long, JobData> jobs = new LinkedHashMap<>();
        Map<Long, StringBuilder> texts = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT j.id, j.title, j.description, j.job_type, j.work_mode, j.city, j.stipend_min, j.stipend_max,
                       j.min_grad_year, j.max_grad_year, j.verified_only
                FROM job j
                WHERE\s""" + where)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JobData j = new JobData();
                    j.setId(rs.getLong("id"));
                    j.setTitle(rs.getString("title"));
                    j.setJobType(JobType.valueOf(rs.getString("job_type")));
                    j.setWorkMode(WorkMode.valueOf(rs.getString("work_mode")));
                    j.setCity(rs.getString("city"));
                    j.setStipendMin(rs.getInt("stipend_min"));
                    j.setStipendMax(rs.getInt("stipend_max"));
                    int min = rs.getInt("min_grad_year");
                    j.setMinGradYear(rs.wasNull() ? null : min);
                    int max = rs.getInt("max_grad_year");
                    j.setMaxGradYear(rs.wasNull() ? null : max);
                    j.setVerifiedOnly(rs.getBoolean("verified_only"));
                    jobs.put(j.getId(), j);
                    texts.put(j.getId(), new StringBuilder(rs.getString("title")).append(' ').append(rs.getString("description")));
                }
            }
        }
        if (jobs.isEmpty()) {
            return List.of();
        }
        // Small tables: read all skill rows once and keep the ones for our jobs (avoids a huge IN list).
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT js.job_id, js.skill_id, js.weight, js.is_required, k.name FROM job_skill js JOIN skill k ON k.id = js.skill_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JobData j = jobs.get(rs.getLong(1));
                if (j != null) {
                    long skillId = rs.getLong(2);
                    j.getSkillWeights().put(skillId, rs.getInt(3));
                    if (rs.getBoolean(4)) {
                        j.getRequiredSkillIds().add(skillId);
                    }
                    texts.get(j.getId()).append(' ').append(rs.getString(5));
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT job_id, college_id FROM job_allowed_college");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JobData j = jobs.get(rs.getLong(1));
                if (j != null) {
                    j.getAllowedCollegeIds().add(rs.getLong(2));
                }
            }
        }
        jobs.values().forEach(j -> j.setText(texts.get(j.getId()).toString()));
        return new ArrayList<>(jobs.values());
    }

    // ---------------------------------------------------------------- score cache

    /** Stores every score component so any card can be explained later (and for the DBMS report). */
    public void saveScores(List<ScoreBreakdown> scores) {
        if (scores.isEmpty()) {
            return;
        }
        withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO match_score_cache (applicant_id, job_id, total, skill_coverage, tfidf_cosine,
                                                   preference_boost, location_fit, stipend_fit, computed_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    ON DUPLICATE KEY UPDATE total = VALUES(total), skill_coverage = VALUES(skill_coverage),
                        tfidf_cosine = VALUES(tfidf_cosine), preference_boost = VALUES(preference_boost),
                        location_fit = VALUES(location_fit), stipend_fit = VALUES(stipend_fit), computed_at = NOW()""")) {
                for (ScoreBreakdown s : scores) {
                    ps.setLong(1, s.getApplicantId());
                    ps.setLong(2, s.getJobId());
                    ps.setDouble(3, s.getTotal());
                    ps.setDouble(4, s.getSkillCoverage());
                    ps.setDouble(5, s.getTfidfCosine());
                    ps.setDouble(6, s.getPreferenceBoost());
                    ps.setDouble(7, s.getLocationFit());
                    ps.setDouble(8, s.getStipendFit());
                    ps.addBatch();
                }
                return ps.executeBatch().length;
            }
        });
    }

    private static void append(StringBuilder sb, String... parts) {
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                sb.append(p).append(' ');
            }
        }
    }
}
