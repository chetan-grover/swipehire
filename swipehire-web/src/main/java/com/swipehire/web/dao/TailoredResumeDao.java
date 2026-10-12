package com.swipehire.web.dao;

import com.swipehire.common.tailoring.TailoredResume;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.List;

/** Records which resume items went into an application's tailored PDF, in order (audit trail). */
@Repository
public class TailoredResumeDao extends JdbcDao {

    public TailoredResumeDao(DataSource dataSource) {
        super(dataSource);
    }

    public void replace(long applicationId, List<TailoredResume.ScoredItem> items) {
        withConnection(con -> {
            try (PreparedStatement del = con.prepareStatement("DELETE FROM tailored_resume_item WHERE application_id = ?")) {
                del.setLong(1, applicationId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO tailored_resume_item (application_id, resume_item_id, rank_order, relevance) VALUES (?, ?, ?, ?)")) {
                int rank = 1;
                for (TailoredResume.ScoredItem s : items) {
                    ins.setLong(1, applicationId);
                    ins.setLong(2, s.item().getId());
                    ins.setInt(3, rank++);
                    ins.setDouble(4, s.relevance());
                    ins.addBatch();
                }
                return ins.executeBatch().length;
            }
        });
    }
}
