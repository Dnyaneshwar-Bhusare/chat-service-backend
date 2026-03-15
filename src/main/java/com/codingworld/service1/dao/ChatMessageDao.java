package com.codingworld.service1.dao;

import com.codingworld.service1.model.ChatMessageEntity;
import com.codingworld.service1.model.ChatMessageView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object (DAO) for Chat Message operations
 */
@Repository
public class ChatMessageDao {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Save chat message to database
     * @param chatMessage ChatMessageEntity to save
     * @return Generated chat_id
     */
    public String saveChatMessage(ChatMessageEntity chatMessage) {
        // Generate unique chat_id
        String chatId = UUID.randomUUID().toString();

        String sql = "INSERT INTO db_chat.chat_table " +
                     "(chat_id, message, `from`, `to`, algo, tx_hash, created_ts) " +
                     "VALUES (:chatId, :message, :fromUser, :toUser, :algo, :txHash, :createdTs)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("chatId", chatId);
        parameters.addValue("message", chatMessage.getMessage());
        parameters.addValue("fromUser", chatMessage.getFromUser());
        parameters.addValue("toUser", chatMessage.getToUser());
        parameters.addValue("algo", chatMessage.getAlgo());
        parameters.addValue("txHash", chatMessage.getTxHash());
        parameters.addValue("createdTs", Timestamp.valueOf(chatMessage.getCreatedTs()));

        namedParameterJdbcTemplate.update(sql, parameters);

        return chatId;
    }

    /**
     * Get messages for a specific user (your provided SQL query)
     * @param userId User ID to get messages for
     * @return List of chat messages for the user
     */
    public List<ChatMessageView> getMessagesForUser(String userId) {
        String sql = "SELECT c.chat_id, c.message, c.`from`, c.`to`, c.algo, c.created_ts, c.tx_hash, u.public_key as public_key " +
                     "FROM db_chat.chat_table c " +
                     "LEFT JOIN db_chat.users u ON c.`from` = u.UserID " +
                     "WHERE c.`to` = :userId OR c.`from` = :userId " +
                     "ORDER BY c.created_ts DESC";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("userId", userId);

        return namedParameterJdbcTemplate.query(sql, parameters, new ChatMessageViewRowMapper());
    }

    /**
     * Get latest messages for a user after a specific timestamp
     * Used for real-time updates
     */
    public List<ChatMessageView> getNewMessagesForUser(String userId, LocalDateTime afterTime) {
        String sql = "SELECT chat_id, message, `from`, `to`, algo, created_ts, tx_hash " +
                     "FROM db_chat.chat_table " +
                     "WHERE (`to` = :userId OR `from` = :userId) " +
                     "AND created_ts > :afterTime " +
                     "ORDER BY created_ts ASC";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("userId", userId);
        parameters.addValue("afterTime", Timestamp.valueOf(afterTime));

        return namedParameterJdbcTemplate.query(sql, parameters, new ChatMessageViewRowMapper());
    }

    /**
     * RowMapper for ChatMessageView
     */
    private static class ChatMessageViewRowMapper implements RowMapper<ChatMessageView> {
        @Override
        public ChatMessageView mapRow(ResultSet rs, int rowNum) throws SQLException {
            ChatMessageView message = new ChatMessageView();
            message.setChatId(rs.getString("chat_id"));
            message.setMessage(rs.getString("message"));
            message.setFromUser(rs.getString("from"));
            message.setToUser(rs.getString("to"));
            message.setAlgo(rs.getString("algo"));
            message.setCreatedTs(rs.getTimestamp("created_ts").toLocalDateTime());
            message.setTxHash(rs.getString("tx_hash"));
            message.setPublicKey(rs.getString("public_key")); // Map sender's public key
            return message;
        }
    }
}
