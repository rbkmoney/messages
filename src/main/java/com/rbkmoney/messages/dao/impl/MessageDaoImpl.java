package com.rbkmoney.messages.dao.impl;

import com.rbkmoney.messages.dao.MessageDao;
import com.rbkmoney.messages.domain.Message;
import com.rbkmoney.messages.exception.DaoException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessageDaoImpl implements MessageDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final MessageRowMapper rowMapper = new MessageRowMapper();

    @Override
    public void saveAll(List<Message> messages) throws DaoException {
        String insertSql = "INSERT INTO msgs.message(id, text, created_date, conversation_id, user_id) " +
                "VALUES (:id, :text, :created_date, :conversation_id, :user_id) " +
                "ON CONFLICT (id) DO NOTHING;"; // immutable messages history!
        try {
            var batchValues = messages.stream()
                    .map(m -> new MapSqlParameterSource()
                            .addValue("id", m.getId())
                            .addValue("text", m.getText())
                            .addValue("created_date", LocalDateTime.ofInstant(m.getCreatedDate(), ZoneOffset.UTC))
                            .addValue("conversation_id", m.getConversationId())
                            .addValue("user_id", m.getUserId())
                            .getValues())
                    .collect(Collectors.toList());
            jdbcTemplate.batchUpdate(insertSql, batchValues.toArray(new Map[messages.size()]));
        } catch (Exception ex) {
            throw new DaoException(ex);
        }
    }

    @Override
    public List<Message> findAllByConversationId(List<String> conversationIds) throws DaoException {
        String selectSql = "SELECT id, text, created_date, conversation_id, user_id FROM msgs.message " +
                "WHERE conversation_id in (:ids)";
        try {
            return jdbcTemplate.query(selectSql, new MapSqlParameterSource("ids", conversationIds), rowMapper);
        } catch (Exception ex) {
            throw new DaoException(ex);
        }
    }

    private static class MessageRowMapper implements RowMapper<Message> {
        @Override
        public Message mapRow(ResultSet rs, int i) throws SQLException {
            String id = rs.getString("id");
            String text = rs.getString("text");
            Instant createdDate = rs.getObject("created_date", LocalDateTime.class)
                    .toInstant(ZoneOffset.UTC);
            String conversationId = rs.getString("conversation_id");
            String userId = rs.getString("user_id");
            return new Message(id, text, createdDate, conversationId, userId);
        }
    }

}
