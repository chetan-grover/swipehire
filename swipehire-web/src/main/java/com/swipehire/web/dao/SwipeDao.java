package com.swipehire.web.dao;

import com.swipehire.common.enums.HirerSwipeSource;
import com.swipehire.common.enums.SwipeDirection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Swipe writes and match detection in raw JDBC (PreparedStatement, batches, FOR UPDATE locks).
 *
 * <h3>How a match is detected safely</h3>
 * An applicant and a hirer may swipe right on the same (applicant, job) at the same moment.
 * If each transaction only checks "did the other side already swipe?", both could miss the other
 * (neither has committed yet) and no match is created. So every swipe transaction first locks
 * the applicant's profile row (SELECT ... FOR UPDATE). Swipes involving the same applicant now run
 * one after another; the second one sees the first one's committed swipe (read with FOR SHARE,
 * which always reads the latest committed row) and creates the match. INSERT IGNORE plus the
 * UNIQUE(applicant_id, job_id) key guarantees at most one match row even in odd cases.
 */
@Repository
public class SwipeDao extends JdbcDao {

    public SwipeDao(DataSource dataSource) {
        super(dataSource);
    }

    public record LastSwipe(long id, long jobId, SwipeDirection direction, LocalDateTime createdAt) {
    }

    public record QueueEntry(SwipeDirection direction, String pitch, LocalDateTime swipedAt) {
    }

    /** Serialises all swipes that involve this applicant. Must be called inside a transaction. */
    public void lockApplicant(long applicantId) {
        withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT user_id FROM applicant_profile WHERE user_id = ? FOR UPDATE")) {
                ps.setLong(1, applicantId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** @return false if the applicant already swiped this job (unique key). */
    public boolean insertApplicantSwipe(long applicantId, long jobId, SwipeDirection direction, String pitch) {
        return withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO applicant_swipe (applicant_id, job_id, direction, pitch) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, applicantId);
                ps.setLong(2, jobId);
                ps.setString(3, direction.name());
                if (pitch == null) {
                    ps.setNull(4, Types.VARCHAR);
                } else {
                    ps.setString(4, pitch);
                }
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                if (isDuplicateKey(e)) {
                    return false;
                }
                throw e;
            }
        });
    }

    /** @return false if this applicant was already reviewed for this job. */
    public boolean insertHirerSwipe(long hirerId, long applicantId, long jobId, SwipeDirection direction,
                                    HirerSwipeSource source) {
        return withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO hirer_swipe (hirer_id, applicant_id, job_id, direction, source) VALUES (?, ?, ?, ?, ?)")) {
                ps.setLong(1, hirerId);
                ps.setLong(2, applicantId);
                ps.setLong(3, jobId);
                ps.setString(4, direction.name());
                ps.setString(5, source.name());
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                if (isDuplicateKey(e)) {
                    return false;
                }
                throw e;
            }
        });
    }

    public Optional<SwipeDirection> findApplicantDirection(long applicantId, long jobId) {
        return findDirection("SELECT direction FROM applicant_swipe WHERE applicant_id = ? AND job_id = ? FOR SHARE",
                applicantId, jobId);
    }

    public Optional<SwipeDirection> findHirerDirection(long applicantId, long jobId) {
        return findDirection("SELECT direction FROM hirer_swipe WHERE applicant_id = ? AND job_id = ? FOR SHARE",
                applicantId, jobId);
    }

    private Optional<SwipeDirection> findDirection(String sql, long applicantId, long jobId) {
        return withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, applicantId);
                ps.setLong(2, jobId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(SwipeDirection.valueOf(rs.getString(1))) : Optional.empty();
                }
            }
        });
    }

    /** @return the new match id, or empty if the match already existed. */
    public Optional<Long> insertMatchIfAbsent(long applicantId, long jobId) {
        return withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT IGNORE INTO job_match (applicant_id, job_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, applicantId);
                ps.setLong(2, jobId);
                if (ps.executeUpdate() == 0) {
                    return Optional.empty();
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return Optional.of(keys.getLong(1));
                }
            }
        });
    }

    public boolean matchExists(long applicantId, long jobId) {
        return withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT 1 FROM job_match WHERE applicant_id = ? AND job_id = ?")) {
                ps.setLong(1, applicantId);
                ps.setLong(2, jobId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public Optional<LastSwipe> findLastSwipe(long applicantId) {
        return withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id, job_id, direction, created_at FROM applicant_swipe WHERE applicant_id = ? ORDER BY created_at DESC, id DESC LIMIT 1")) {
                ps.setLong(1, applicantId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new LastSwipe(rs.getLong(1), rs.getLong(2),
                            SwipeDirection.valueOf(rs.getString(3)), rs.getTimestamp(4).toLocalDateTime()));
                }
            }
        });
    }

    public void deleteApplicantSwipe(long swipeId) {
        withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM applicant_swipe WHERE id = ?")) {
                ps.setLong(1, swipeId);
                return ps.executeUpdate();
            }
        });
    }

    public Set<Long> swipedJobIds(long applicantId) {
        return idSet("SELECT job_id FROM applicant_swipe WHERE applicant_id = ?", applicantId);
    }

    /** Jobs whose hirer already liked this applicant (from Discover): highlighted in the deck. */
    public Set<Long> jobsWhereHirerLiked(long applicantId) {
        return idSet("SELECT job_id FROM hirer_swipe WHERE applicant_id = ? AND direction = 'RIGHT'", applicantId);
    }

    private Set<Long> idSet(String sql, long param) {
        return withConnection(con -> {
            Set<Long> ids = new HashSet<>();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, param);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getLong(1));
                    }
                }
            }
            return ids;
        });
    }

    /** Applicants who liked the job and are still waiting for the hirer's decision. Super-swipes first. */
    public Map<Long, QueueEntry> queueCandidates(long jobId) {
        return withConnection(con -> {
            Map<Long, QueueEntry> result = new LinkedHashMap<>();
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT s.applicant_id, s.direction, s.pitch, s.created_at
                    FROM applicant_swipe s
                    JOIN users u ON u.id = s.applicant_id AND u.enabled = TRUE
                    WHERE s.job_id = ? AND s.direction IN ('RIGHT', 'SUPER')
                      AND NOT EXISTS (SELECT 1 FROM hirer_swipe h WHERE h.applicant_id = s.applicant_id AND h.job_id = s.job_id)
                    ORDER BY (s.direction = 'SUPER') DESC, s.created_at""")) {
                ps.setLong(1, jobId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.put(rs.getLong(1), new QueueEntry(SwipeDirection.valueOf(rs.getString(2)),
                                rs.getString(3), rs.getTimestamp(4).toLocalDateTime()));
                    }
                }
            }
            return result;
        });
    }

    // ---------- learned preferences ----------

    public Map<Long, Double> loadAffinity(long applicantId) {
        return withConnection(con -> {
            Map<Long, Double> m = new HashMap<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT skill_id, affinity FROM applicant_skill_pref WHERE applicant_id = ?")) {
                ps.setLong(1, applicantId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        m.put(rs.getLong(1), rs.getDouble(2));
                    }
                }
            }
            return m;
        });
    }

    /** One round trip for all skills of a job, using a JDBC batch. */
    public void upsertAffinities(long applicantId, Map<Long, Double> affinities) {
        if (affinities.isEmpty()) {
            return;
        }
        withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO applicant_skill_pref (applicant_id, skill_id, affinity) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE affinity = VALUES(affinity)""")) {
                for (Map.Entry<Long, Double> e : affinities.entrySet()) {
                    ps.setLong(1, applicantId);
                    ps.setLong(2, e.getKey());
                    ps.setDouble(3, e.getValue());
                    ps.addBatch();
                }
                return ps.executeBatch().length;
            }
        });
    }

    /** Daily job: old preferences fade (x0.98 per day). */
    public int decayAffinities(double factor) {
        return withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE applicant_skill_pref SET affinity = affinity * ? WHERE affinity <> 0")) {
                ps.setDouble(1, factor);
                return ps.executeUpdate();
            }
        });
    }
}
