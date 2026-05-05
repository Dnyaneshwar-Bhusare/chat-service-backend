package com.codingworld.service1.dao;

import com.codingworld.service1.constants.QueryConstants;
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

@Repository
public class ChatMessageDao {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public String saveChatMessage(ChatMessageEntity chatMessage) {
        String chatId = UUID.randomUUID().toString();

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("chatId", chatId);
        parameters.addValue("message", chatMessage.getMessage());
        parameters.addValue("messageToSelf", chatMessage.getMessageToSelf());
        parameters.addValue("fromUser", chatMessage.getFromUser());
        parameters.addValue("toUser", chatMessage.getToUser());
        parameters.addValue("algo", chatMessage.getAlgo());
        parameters.addValue("txHash", chatMessage.getTxHash());
        parameters.addValue("createdTs", Timestamp.valueOf(chatMessage.getCreatedTs()));

        namedParameterJdbcTemplate.update(QueryConstants.CHAT_INSERT, parameters);
        return chatId;
    }

    public List<ChatMessageView> getMessagesForUser(String userId, String sender, String reciver) {
        if ((sender == null || sender.trim().isEmpty()) && (reciver == null || reciver.trim().isEmpty())) {
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("userId", userId);
            return namedParameterJdbcTemplate.query(QueryConstants.CHAT_GET_ALL_FOR_USER, parameters, new ChatMessageViewRowMapperSimple());
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("sender", sender);
        parameters.addValue("reciver", reciver);
        return namedParameterJdbcTemplate.query(QueryConstants.CHAT_GET_BETWEEN_USERS, parameters, new ChatMessageViewRowMapper());
    }

    public List<ChatMessageView> getNewMessagesForUser(String userId, LocalDateTime afterTime) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("userId", userId);
        parameters.addValue("afterTime", Timestamp.valueOf(afterTime));
        return namedParameterJdbcTemplate.query(QueryConstants.CHAT_GET_NEW_FOR_USER, parameters, new ChatMessageViewRowMapper());
    }

    public ChatMessageView getMessageByChatId(String chatId) {
        MapSqlParameterSource params = new MapSqlParameterSource("chatId", chatId);
        List<ChatMessageView> results = namedParameterJdbcTemplate.query(QueryConstants.CHAT_GET_BY_ID, params, new ChatMessageViewRowMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean deleteMessageById(String chatId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("chatId", chatId);
        int rows = namedParameterJdbcTemplate.update(QueryConstants.CHAT_DELETE_BY_ID, params);
        return rows > 0;
    }

    public int deleteConversation(String userId1, String userId2) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId1", userId1);
        params.addValue("userId2", userId2);
        return namedParameterJdbcTemplate.update(QueryConstants.CHAT_DELETE_CONVERSATION, params);
    }

    public int deleteAllMessagesForUser(String userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return namedParameterJdbcTemplate.update(QueryConstants.CHAT_DELETE_ALL_FOR_USER, params);
    }

    public boolean updateMessageByChatId(String chatId, String message, String messageToSelf, java.time.LocalDateTime updatedTs) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("chatId", chatId);
        params.addValue("message", message);
        params.addValue("messageToSelf", messageToSelf);
        params.addValue("updatedTs", java.sql.Timestamp.valueOf(updatedTs));
        int rows = namedParameterJdbcTemplate.update(QueryConstants.CHAT_UPDATE_MESSAGE_BY_ID, params);
        return rows > 0;
    }

    private static class ChatMessageViewRowMapper implements RowMapper<ChatMessageView> {
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
