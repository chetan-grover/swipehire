package com.swipehire.web.dao;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Report queries in plain SQL (aggregates, GROUP BY, subqueries, the responsiveness view).
 * These are also the queries shown in the DBMS report.
 */
@Repository
public class AnalyticsDao extends JdbcDao {

    public AnalyticsDao(DataSource dataSource) {
        super(dataSource);
    }

    public record DailyActivity(LocalDate day, long swipes, long matches) {
    }

    /** hirerId -> % of decided applications acted on within 7 days (from v_hirer_responsiveness). */
    public Map<Long, Integer> responsiveness(Collection<Long> hirerIds) {
        Map<Long, Integer> result = new HashMap<>();
        if (hirerIds.isEmpty()) {
            return result;
        }
        return withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT hirer_id, decided_applications, acted_in_time FROM v_hirer_responsiveness");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long hirer = rs.getLong(1);
                    long decided = rs.getLong(2);
                    if (hirerIds.contains(hirer) && decided > 0) {
                        result.put(hirer, (int) Math.round(100.0 * rs.getLong(3) / decided));
                    }
                }
            }
            return result;
        });
    }

    /** Per-job hiring funnel, from "scored for" down to offers. */
    public Map<String, Long> jobFunnel(long jobId) {
        return withConnection(con -> {
            Map<String, Long> f = new LinkedHashMap<>();
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT
                      (SELECT COUNT(*) FROM match_score_cache WHERE job_id = ?)                                  AS shown,
                      (SELECT COUNT(*) FROM applicant_swipe WHERE job_id = ? AND direction IN ('RIGHT','SUPER')) AS liked,
                      (SELECT COUNT(*) FROM hirer_swipe WHERE job_id = ? AND direction = 'RIGHT')                AS hirer_liked,
                      (SELECT COUNT(*) FROM job_match WHERE job_id = ?)                                          AS matched,
                      (SELECT COUNT(*) FROM application a JOIN job_match m ON m.id = a.match_id
                         WHERE m.job_id = ? AND a.applied_at IS NOT NULL)                                       AS applied,
                      (SELECT COUNT(DISTINCT h.application_id) FROM application_status_history h
                         JOIN application a ON a.id = h.application_id JOIN job_match m ON m.id = a.match_id
                         WHERE m.job_id = ? AND h.to_status = 'SHORTLISTED')                                    AS shortlisted,
                      (SELECT COUNT(*) FROM application a JOIN job_match m ON m.id = a.match_id
                         WHERE m.job_id = ? AND a.status = 'OFFER')                                             AS offers""")) {
                for (int i = 1; i <= 7; i++) {
                    ps.setLong(i, jobId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    f.put("Shown", rs.getLong("shown"));
                    f.put("Liked by applicants", rs.getLong("liked"));
                    f.put("Liked by you", rs.getLong("hirer_liked"));
                    f.put("Matched", rs.getLong("matched"));
                    f.put("Applied", rs.getLong("applied"));
                    f.put("Shortlisted", rs.getLong("shortlisted"));
                    f.put("Offers", rs.getLong("offers"));
                }
            }
            return f;
        });
    }

    /** For each skill of the job: % of interested applicants who have it. Low % = hard-to-find skill. */
    public Map<String, Double> skillGap(long jobId) {
        return withConnection(con -> {
            Map<String, Double> m = new LinkedHashMap<>();
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT k.name,
                           100.0 * COUNT(ask.applicant_id) / NULLIF((SELECT COUNT(*) FROM applicant_swipe
                                                     WHERE job_id = ? AND direction IN ('RIGHT','SUPER')), 0) AS pct
                    FROM job_skill js
                    JOIN skill k ON k.id = js.skill_id
                    LEFT JOIN applicant_swipe s ON s.job_id = js.job_id AND s.direction IN ('RIGHT','SUPER')
                    LEFT JOIN applicant_skill ask ON ask.applicant_id = s.applicant_id AND ask.skill_id = js.skill_id
                    WHERE js.job_id = ?
                    GROUP BY k.name, js.weight
                    ORDER BY js.weight DESC, k.name""")) {
                ps.setLong(1, jobId);
                ps.setLong(2, jobId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        m.put(rs.getString(1), rs.getDouble(2));
                    }
                }
            }
            return m;
        });
    }

    /** Average hours from "applicant applied" to the hirer's first status change, for one hirer. */
    public Double avgResponseHours(long hirerId) {
        return withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT AVG(TIMESTAMPDIFF(MINUTE, a.applied_at, first_action.changed_at)) / 60.0
                    FROM application a
                    JOIN job_match m ON m.id = a.match_id
                    JOIN job j ON j.id = m.job_id
                    JOIN (SELECT application_id, MIN(changed_at) AS changed_at
                          FROM application_status_history WHERE changed_by = ? GROUP BY application_id) first_action
                      ON first_action.application_id = a.id
                    WHERE j.posted_by = ? AND a.applied_at IS NOT NULL""")) {
                ps.setLong(1, hirerId);
                ps.setLong(2, hirerId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    double v = rs.getDouble(1);
                    return rs.wasNull() ? null : v;
                }
            }
        });
    }

    /** Required skills the applicant lacks, counted over the jobs they liked: "what should I learn?" */
    public Map<String, Long> applicantMissingSkills(long applicantId, int limit) {
        return withConnection(con -> {
            Map<String, Long> m = new LinkedHashMap<>();
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT k.name, COUNT(*) AS jobs
                    FROM applicant_swipe s
                    JOIN job_skill js ON js.job_id = s.job_id AND js.is_required = TRUE
                    JOIN skill k ON k.id = js.skill_id
                    WHERE s.applicant_id = ? AND s.direction IN ('RIGHT','SUPER')
                      AND NOT EXISTS (SELECT 1 FROM applicant_skill a WHERE a.applicant_id = s.applicant_id AND a.skill_id = js.skill_id)
                    GROUP BY k.name
                    ORDER BY jobs DESC, k.name
                    LIMIT ?""")) {
                ps.setLong(1, applicantId);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        m.put(rs.getString(1), rs.getLong(2));
                    }
                }
            }
            return m;
        });
    }

    public Map<String, Long> applicantStats(long applicantId) {
        return withConnection(con -> {
            Map<String, Long> m = new LinkedHashMap<>();
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT
                      (SELECT COUNT(*) FROM applicant_swipe WHERE applicant_id = ?)                                  AS swipes,
                      (SELECT COUNT(*) FROM applicant_swipe WHERE applicant_id = ? AND direction IN ('RIGHT','SUPER')) AS likes,
                      (SELECT COUNT(*) FROM job_match WHERE applicant_id = ?)                                        AS matches,
                      (SELECT COUNT(*) FROM application a JOIN job_match mm ON mm.id = a.match_id
                         WHERE mm.applicant_id = ? AND a.status IN ('SHORTLISTED','INTERVIEW','OFFER'))             AS progressing""")) {
                for (int i = 1; i <= 4; i++) {
                    ps.setLong(i, applicantId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    m.put("Swipes", rs.getLong(1));
                    m.put("Likes", rs.getLong(2));
                    m.put("Matches", rs.getLong(3));
                    m.put("Shortlisted or further", rs.getLong(4));
                }
            }
            return m;
        });
    }

    /** Platform activity for the admin chart: swipes (both sides) and matches per day. */
    public List<DailyActivity> dailyActivity(int days) {
        return withConnection(con -> {
            Map<LocalDate, long[]> byDay = new LinkedHashMap<>();
            LocalDate start = LocalDate.now().minusDays(days - 1L);
            for (int i = 0; i < days; i++) {
                byDay.put(start.plusDays(i), new long[2]);
            }
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT d, SUM(swipes), SUM(matches) FROM (
                        SELECT DATE(created_at) d, COUNT(*) swipes, 0 matches FROM applicant_swipe WHERE created_at >= ? GROUP BY DATE(created_at)
                        UNION ALL
                        SELECT DATE(created_at), COUNT(*), 0 FROM hirer_swipe WHERE created_at >= ? GROUP BY DATE(created_at)
                        UNION ALL
                        SELECT DATE(matched_at), 0, COUNT(*) FROM job_match WHERE matched_at >= ? GROUP BY DATE(matched_at)
                    ) t GROUP BY d ORDER BY d""")) {
                for (int i = 1; i <= 3; i++) {
                    ps.setObject(i, start.atStartOfDay());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long[] v = byDay.get(rs.getDate(1).toLocalDate());
                        if (v != null) {
                            v[0] = rs.getLong(2);
                            v[1] = rs.getLong(3);
                        }
                    }
                }
            }
            List<DailyActivity> list = new ArrayList<>();
            byDay.forEach((d, v) -> list.add(new DailyActivity(d, v[0], v[1])));
            return list;
        });
    }

    /** jobId -> {applicants waiting in the queue, matches} for the hirer dashboard. */
    public Map<Long, long[]> jobCounters(long companyId) {
        return withConnection(con -> {
            Map<Long, long[]> m = new HashMap<>();
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT j.id,
                      (SELECT COUNT(*) FROM applicant_swipe s WHERE s.job_id = j.id AND s.direction IN ('RIGHT','SUPER')
                         AND NOT EXISTS (SELECT 1 FROM hirer_swipe h WHERE h.job_id = s.job_id AND h.applicant_id = s.applicant_id)) AS waiting,
                      (SELECT COUNT(*) FROM job_match mm WHERE mm.job_id = j.id) AS matches
                    FROM job j WHERE j.company_id = ?""")) {
                ps.setLong(1, companyId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        m.put(rs.getLong(1), new long[]{rs.getLong(2), rs.getLong(3)});
                    }
                }
            }
            return m;
        });
    }

    /** Rows for the CSV export of one job's pipeline. */
    public List<String[]> pipelineRows(long jobId) {
        return withConnection(con -> {
            List<String[]> rows = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT u.full_name, u.email, c.name AS college, p.grad_year, a.status,
                           ROUND(sc.total * 100) AS score, m.matched_at, a.applied_at, a.status_changed_at
                    FROM application a
                    JOIN job_match m ON m.id = a.match_id
                    JOIN users u ON u.id = m.applicant_id
                    JOIN applicant_profile p ON p.user_id = m.applicant_id
                    LEFT JOIN college c ON c.id = p.college_id AND p.college_verified_at IS NOT NULL
                    LEFT JOIN match_score_cache sc ON sc.applicant_id = m.applicant_id AND sc.job_id = m.job_id
                    WHERE m.job_id = ?
                    ORDER BY a.status_changed_at DESC""")) {
                ps.setLong(1, jobId);
                try (ResultSet rs = ps.executeQuery()) {
                    int cols = rs.getMetaData().getColumnCount();
                    while (rs.next()) {
                        String[] row = new String[cols];
                        for (int i = 0; i < cols; i++) {
                            row[i] = rs.getString(i + 1);
                        }
                        rows.add(row);
                    }
                }
            }
            return rows;
        });
    }

    /** Score breakdown stored for one pair (shown on the hirer's application page). */
    public double[] storedScore(long applicantId, long jobId) {
        return withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT total, skill_coverage, tfidf_cosine, preference_boost, location_fit, stipend_fit
                    FROM match_score_cache WHERE applicant_id = ? AND job_id = ?""")) {
                ps.setLong(1, applicantId);
                ps.setLong(2, jobId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    double[] v = new double[6];
                    for (int i = 0; i < 6; i++) {
                        v[i] = rs.getDouble(i + 1);
                    }
                    return v;
                }
            }
        });
    }
}
