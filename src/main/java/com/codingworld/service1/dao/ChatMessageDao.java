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
                     "(chat_id, message,message_1, `from`, `to`, algo, tx_hash, created_ts) " +
                     "VALUES (:chatId, :message,:messageToSelf, :fromUser, :toUser, :algo, :txHash, :createdTs)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("chatId", chatId);
        parameters.addValue("message", chatMessage.getMessage());
        parameters.addValue("fromUser", chatMessage.getFromUser());
        parameters.addValue("toUser", chatMessage.getToUser());
        parameters.addValue("algo", chatMessage.getAlgo());
        parameters.addValue("txHash", chatMessage.getTxHash());
        parameters.addValue("createdTs", Timestamp.valueOf(chatMessage.getCreatedTs()));
        parameters.addValue("messageToSelf", chatMessage.getMessageToSelf());

        namedParameterJdbcTemplate.update(sql, parameters);

        return chatId;
    }

    /**
     * Get messages for a specific user (your provided SQL query)
     * @param userId User ID to get messages for
     * @return List of chat messages for the user
     */
    public List<ChatMessageView> getMessagesForUser(String userId, String sender, String reciver) {
        if ((sender == null || sender.trim().isEmpty()) && (reciver == null || reciver.trim().isEmpty())) {
            // If both sender and reciver are null or empty, use the simple query
            String sql = "SELECT c.chat_id, c.message, " +
                        "    CASE \n" +
                        "        WHEN c.`from` = :userId THEN c.`message_1`\n" +
                        "        ELSE c.`message`\n" +
                        "    END AS message_for_sender,\n" +
                         "c.`from`, c.`to`, c.algo, c.created_ts, c.tx_hash, u.public_key as public_key " +
                         "FROM db_chat.chat_table c " +
                         "LEFT JOIN db_chat.users u ON c.`from` = u.UserID " +
                         "WHERE c.`to` = :userId OR c.`from` = :userId " +
                         "ORDER BY c.created_ts DESC";
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("userId", userId);
            return namedParameterJdbcTemplate.query(sql, parameters, new ChatMessageViewRowMapperSimple());
        }
        String sqlUpdated = "SELECT \n" +
                "    c.chat_id,\n" +
                "    CASE \n" +
                "        WHEN c.`from` = :sender THEN c.`message_1`\n" +
                "        WHEN c.`to` = :sender THEN c.`message`\n" +
                "    END AS message_for_sender,\n" +
                "    c.`from`,\n" +
                "    c.`to`,\n" +
                "    c.algo,\n" +
                "    c.created_ts,\n" +
                "    c.tx_hash,\n" +
                "    u.public_key\n" +
                "FROM db_chat.chat_table c\n" +
                "LEFT JOIN db_chat.users u \n" +
                "    ON c.`from` = u.UserID\n" +
                "WHERE \n" +
                "    (\n" +
                "        (c.`from` = :sender AND c.`to` = :reciver)\n" +
                "        OR \n" +
                "        (c.`from` = :reciver AND c.`to` = :sender)\n" +
                "    )\n" +
                "ORDER BY c.created_ts DESC";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("sender", sender);
        parameters.addValue("reciver", reciver);

        return namedParameterJdbcTemplate.query(sqlUpdated, parameters, new ChatMessageViewRowMapper());
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
     * Fetch a single message row by its chat_id.
     * Returns null if not found.
     */
    public ChatMessageView getMessageByChatId(String chatId) {
        String sql = "SELECT c.chat_id, c.message AS message_for_sender, " +
                     "c.`from`, c.`to`, c.algo, c.created_ts, c.tx_hash, " +
                     "u.public_key " +
                     "FROM db_chat.chat_table c " +
                     "LEFT JOIN db_chat.users u ON c.`from` = u.UserID " +
                     "WHERE c.chat_id = :chatId";

        MapSqlParameterSource params = new MapSqlParameterSource("chatId", chatId);
        List<ChatMessageView> results = namedParameterJdbcTemplate.query(sql, params, new ChatMessageViewRowMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * RowMapper for ChatMessageView
     */
    private static class ChatMessageViewRowMapper implements RowMapper<ChatMessageView> {
        @Override
        public ChatMessageView mapRow(ResultSet rs, int rowNum) throws SQLException {
            ChatMessageView message = new ChatMessageView();
            message.setChatId(rs.getString("chat_id"));
            message.setMessage(rs.getString("message_for_sender")); // Use the correct alias from SQL
            message.setFromUser(rs.getString("from"));
            message.setToUser(rs.getString("to"));
            message.setAlgo(rs.getString("algo"));
            message.setCreatedTs(rs.getTimestamp("created_ts").toLocalDateTime());
            message.setTxHash(rs.getString("tx_hash"));
            message.setPublicKey(rs.getString("public_key")); // Map sender's public key
            return message;
        }
    }

    // Simple RowMapper for the basic query
    private static class ChatMessageViewRowMapperSimple implements RowMapper<ChatMessageView> {
        @Override
        public ChatMessageView mapRow(ResultSet rs, int rowNum) throws SQLException {
            ChatMessageView message = new ChatMessageView();
            message.setChatId(rs.getString("chat_id"));
            message.setMessage(rs.getString("message_for_sender"));
            message.setFromUser(rs.getString("from"));
            message.setToUser(rs.getString("to"));
            message.setAlgo(rs.getString("algo"));
            message.setCreatedTs(rs.getTimestamp("created_ts").toLocalDateTime());
            message.setTxHash(rs.getString("tx_hash"));
            message.setPublicKey(rs.getString("public_key"));
            return message;
        }
    }
}
