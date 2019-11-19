package com.rbkmoney.messages.dao.impl;

import com.rbkmoney.messages.dao.DaoHelper;
import com.rbkmoney.messages.dao.UserDao;
import com.rbkmoney.messages.domain.User;
import com.rbkmoney.messages.exception.DaoException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserDaoImpl implements UserDao {

    private final JdbcTemplate jdbcTemplate;
    private final DaoHelper daoHelper;

    private final UserRowMapper rowMapper = new UserRowMapper();

    @Override
    public void saveAll(List<User> users) throws DaoException {
        String upsertSql = "INSERT INTO msgs.author(id, email, full_name) VALUES (?, ?, ?) " +
                "ON CONFLICT (id) DO " +
                "UPDATE set email = excluded.email, full_name = excluded.full_name;";
        int[][] updateCounts;

        try {
            updateCounts = jdbcTemplate.batchUpdate(upsertSql, users, daoHelper.batchSize, (ps, argument) -> {
                ps.setString(1, argument.getId());
                ps.setString(2, argument.getEmail());
                ps.setString(3, argument.getFullName());
            });
        } catch (Exception ex) {
            throw new DaoException(ex);
        }

        daoHelper.checkUpdateRowsSuccess(updateCounts);
    }

    @Override
    public void save(User user) throws DaoException {
        String insertSql = "INSERT INTO msgs.author(id, email, full_name) VALUES (?, ?, ?) " +
                "ON CONFLICT (id) DO " +
                "UPDATE set email = excluded.email, full_name = excluded.full_name";
        try {
            jdbcTemplate.execute(insertSql, (PreparedStatementCallback<Boolean>) ps -> {
                ps.setString(1, user.getId());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getFullName());

                return ps.execute();
            });
        } catch (Exception ex) {
            throw new DaoException(ex);
        }
    }

    @Override
    public List<User> findAllById(List<String> ids) throws DaoException {
        String selectSql = "SELECT id, email, full_name FROM msgs.author " +
                "WHERE id in " + daoHelper.collectToIdsCollection(ids);
        try {
            return jdbcTemplate.query(selectSql, rowMapper);
        } catch (Exception ex) {
            throw new DaoException(ex);
        }
    }

    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int i) throws SQLException {
            String id = rs.getString("id");
            String email = rs.getString("email");
            String fullName = rs.getString("full_name");
            return new User(id, email, fullName);
        }
    }
}
