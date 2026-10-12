package com.swipehire.web.dao;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

/**
 * Daily swipe quota with an atomic check-and-increment.
 * <p>
 * The naive way ("SELECT used; if used &lt; 25 then UPDATE used+1") has a race: two requests
 * can both read 24 and both write 25 -> 26 swipes. Instead we let MySQL do the check inside one
 * UPDATE: "... SET right_used = right_used + 1 WHERE ... AND right_used &lt; 25". The row lock
 * taken by UPDATE serialises concurrent requests, and 0 affected rows means "limit reached".
 */
@Repository
public class QuotaDao extends JdbcDao {

    public static final int RIGHT_LIMIT = 25;
    public static final int SUPER_LIMIT = 3;

    public QuotaDao(DataSource dataSource) {
        super(dataSource);
    }

    /** @return true if a swipe of this kind was available and has now been used. */
    public boolean tryConsume(long applicantId, LocalDate day, boolean superSwipe) {
        return withConnection(con -> {
            try (PreparedStatement ensure = con.prepareStatement(
                    "INSERT IGNORE INTO daily_quota (applicant_id, quota_date) VALUES (?, ?)")) {
                ensure.setLong(1, applicantId);
                ensure.setDate(2, Date.valueOf(day));
                ensure.executeUpdate();
            }
            String sql = superSwipe
                    ? "UPDATE daily_quota SET super_used = super_used + 1 WHERE applicant_id = ? AND quota_date = ? AND super_used < ?"
                    : "UPDATE daily_quota SET right_used = right_used + 1 WHERE applicant_id = ? AND quota_date = ? AND right_used < ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, applicantId);
                ps.setDate(2, Date.valueOf(day));
                ps.setInt(3, superSwipe ? SUPER_LIMIT : RIGHT_LIMIT);
                return ps.executeUpdate() == 1;
            }
        });
    }

    /** Gives a swipe back (used by undo). Never goes below zero. */
    public void refund(long applicantId, LocalDate day, boolean superSwipe) {
        String sql = superSwipe
                ? "UPDATE daily_quota SET super_used = super_used - 1 WHERE applicant_id = ? AND quota_date = ? AND super_used > 0"
                : "UPDATE daily_quota SET right_used = right_used - 1 WHERE applicant_id = ? AND quota_date = ? AND right_used > 0";
        withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, applicantId);
                ps.setDate(2, Date.valueOf(day));
                return ps.executeUpdate();
            }
        });
    }

    /** @return {rightUsed, superUsed} for the day (zeros if no row yet). */
    public int[] usage(long applicantId, LocalDate day) {
        return withConnection(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT right_used, super_used FROM daily_quota WHERE applicant_id = ? AND quota_date = ?")) {
                ps.setLong(1, applicantId);
                ps.setDate(2, Date.valueOf(day));
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new int[]{rs.getInt(1), rs.getInt(2)} : new int[]{0, 0};
                }
            }
        });
    }
}
