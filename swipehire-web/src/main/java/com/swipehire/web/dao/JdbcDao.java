package com.swipehire.web.dao;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base class for our plain-JDBC DAOs.
 * <p>
 * Why DataSourceUtils instead of dataSource.getConnection()? If a Spring @Transactional method is
 * running, DataSourceUtils hands us the SAME connection that Hibernate is using, so our raw
 * PreparedStatements and the JPA calls commit or roll back together. Outside a transaction it
 * simply borrows a pooled connection and returns it afterwards.
 */
public abstract class JdbcDao {

    protected final DataSource dataSource;

    protected JdbcDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @FunctionalInterface
    protected interface SqlWork<T> {
        T run(Connection con) throws SQLException;
    }

    protected <T> T withConnection(SqlWork<T> work) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        try {
            return work.run(con);
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("SQL failed: " + e.getMessage(), e);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    /** MySQL error 1062 = duplicate key: the UNIQUE constraint did its job. */
    protected static boolean isDuplicateKey(SQLException e) {
        return e.getErrorCode() == 1062;
    }
}
