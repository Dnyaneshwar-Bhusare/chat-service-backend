package com.codingworld.service1.dao;

import com.codingworld.service1.constants.QueryConstants;
import com.codingworld.service1.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class NotificationDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public void insertNotification(Notification notification) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", notification.getNotificationType());
        params.addValue("refId", notification.getNotificationRefId());
        params.addValue("timestamp", notification.getTimestamp());
        jdbcTemplate.update(QueryConstants.NOTIFICATION_INSERT, params);
    }

    public List<Notification> getAllNotifications() {
        return jdbcTemplate.query(QueryConstants.NOTIFICATION_GET_ALL, new NotificationRowMapper());
    }

    public List<Notification> getNotificationsByTypeAndRefId(String type, String refId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", type);
        params.addValue("refId", refId);
        List<Notification> notifications = jdbcTemplate.query(QueryConstants.NOTIFICATION_GET_BY_TYPE_AND_REF, params, new NotificationRowMapper());

        for (Notification notification : notifications) {
            if ("DECRYPT_FAILURE".equalsIgnoreCase(notification.getNotificationType())) {
                String ts = notification.getTimestamp();
                notification.setMessage("MITM Attack detected at " + (ts != null ? ts : "unknown time"));
            } else if ("LOGIN".equalsIgnoreCase(notification.getNotificationType())) {
                String ts = notification.getTimestamp();
                notification.setMessage("Login Activity detected at " + (ts != null ? ts : "unknown time"));
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
