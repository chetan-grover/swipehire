package com.swipehire.web.dao;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plain JDBC (no JdbcTemplate) on purpose: shows Connection -> PreparedStatement -> ResultSet
 * with try-with-resources, so every resource is closed even if a query fails.
 */
@Repository
public class PlatformStatsDao {

    private static final String SQL = """
            SELECT (SELECT COUNT(*) FROM job WHERE status = ?)       AS active_jobs,
                   (SELECT COUNT(*) FROM applicant_profile)          AS applicants,
                   (SELECT COUNT(*) FROM company WHERE status = ?)   AS verified_companies,
                   (SELECT COUNT(*) FROM skill)                      AS skills,
                   (SELECT COUNT(*) FROM job_match)                  AS matches
            """;

    private final DataSource dataSource;

    public PlatformStatsDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Returns label -> count, in display order, for the landing page. */
    public Map<String, Long> loadStats() throws SQLException {
        Map<String, Long> stats = new LinkedHashMap<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {
            ps.setString(1, "ACTIVE");
            ps.setString(2, "VERIFIED");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("Active jobs", rs.getLong("active_jobs"));
                    stats.put("Applicants", rs.getLong("applicants"));
                    stats.put("Verified companies", rs.getLong("verified_companies"));
                    stats.put("Skills in dictionary", rs.getLong("skills"));
                    stats.put("Matches made", rs.getLong("matches"));
                }
            }
        }
        return stats;
    }
}
