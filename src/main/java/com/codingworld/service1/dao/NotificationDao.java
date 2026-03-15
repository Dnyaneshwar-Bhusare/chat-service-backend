package com.codingworld.service1.dao;

import com.codingworld.service1.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class NotificationDao {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public void insertNotification(Notification notification) {
        String sql = "INSERT INTO notification_table (notification_type, notification_ref_id, timestamp) VALUES (:type, :refId, :timestamp)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", notification.getNotificationType());
        params.addValue("refId", notification.getNotificationRefId());
        params.addValue("timestamp", notification.getTimestamp());
        jdbcTemplate.update(sql, params);
    }

    public List<Notification> getAllNotifications() {
        String sql = "SELECT notification_type, notification_ref_id, timestamp FROM notification_table";
        return jdbcTemplate.query(sql, new NotificationRowMapper());
    }

    public List<Notification> getNotificationsByTypeAndRefId(String type, String refId) {
        String sql = "SELECT notification_type, notification_ref_id, timestamp FROM notification_table WHERE notification_type = :type AND notification_ref_id = :refId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", type);
        params.addValue("refId", refId);
        List<Notification> notifications = jdbcTemplate.query(sql, params, new NotificationRowMapper());
        // If type is 'login', add a message to each notification
        if ("login".equalsIgnoreCase(type)) {
            for (Notification notification : notifications) {
                String ts = notification.getTimestamp();
                notification.setMessage("Login activity detected at " + (ts != null ? ts : "unknown time"));
            }
        }
        return notifications;
    }

    private static class NotificationRowMapper implements RowMapper<Notification> {
        @Override
        public Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
            Notification notification = new Notification();
            notification.setNotificationType(rs.getString("notification_type"));
            notification.setNotificationRefId(rs.getString("notification_ref_id"));
            notification.setTimestamp(rs.getString("timestamp"));
            return notification;
        }
    }
}
