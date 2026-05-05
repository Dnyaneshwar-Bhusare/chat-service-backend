package com.codingworld.service1.dao;

import com.codingworld.service1.constants.QueryConstants;
import com.codingworld.service1.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UserDao {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<User> getAllUsers() {
        return namedParameterJdbcTemplate.query(QueryConstants.USER_GET_ALL, new MapSqlParameterSource(), new UserRowMapper());
    }

    public User getUserByEmail(String email) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("email", email);
        List<User> users = namedParameterJdbcTemplate.query(QueryConstants.USER_GET_BY_EMAIL, parameters, new UserRowMapper());
        return users.isEmpty() ? null : users.get(0);
    }

    public User getUserByUsername(String username) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("username", username);
        List<User> users = namedParameterJdbcTemplate.query(QueryConstants.USER_GET_BY_USERNAME, parameters, new UserRowMapper());
        return users.isEmpty() ? null : users.get(0);
    }

    public User getUserCredentials(String email) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("email", email);
        List<User> users = namedParameterJdbcTemplate.query(QueryConstants.USER_GET_CREDENTIALS, parameters, new UserCredentialsRowMapper());
        return users.isEmpty() ? null : users.get(0);
    }

    public User getUserForLogin(String email) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("email", email);
        List<User> users = namedParameterJdbcTemplate.query(QueryConstants.USER_GET_FOR_LOGIN, parameters, new UserLoginRowMapper());
        return users.isEmpty() ? null : users.get(0);
    }

    public User getUserById(String userId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("userId", userId);
        List<User> users = namedParameterJdbcTemplate.query(QueryConstants.USER_GET_BY_ID, parameters, new UserLoginRowMapper());
        return users.isEmpty() ? null : users.get(0);
    }

    public boolean insertUser(User user) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("username", user.getUsername());
        params.addValue("email", user.getEmail());
        params.addValue("password", user.getPassword());
        params.addValue("profilePic", user.getProfilePic());
        params.addValue("mobileno", user.getMobileno());
        params.addValue("publicKey", user.getPublicKey());
        int rows = namedParameterJdbcTemplate.update(QueryConstants.USER_INSERT, params);
        return rows > 0;
    }

    public void updatePassword(String email, String hashedPassword) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("password", hashedPassword);
        params.addValue("email", email);
        namedParameterJdbcTemplate.update(QueryConstants.USER_UPDATE_PASSWORD, params);
    }

    public void updateUserPublicKey(String email, String publicKey) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("publicKey", publicKey);
        params.addValue("email", email);
        namedParameterJdbcTemplate.update(QueryConstants.USER_UPDATE_PUBLIC_KEY, params);
    }

    public void updateLastSeen(String userId, java.time.LocalDateTime lastSeen) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("lastSeen", lastSeen);
        namedParameterJdbcTemplate.update(QueryConstants.USER_UPDATE_LAST_SEEN, params);
    }

    public java.time.LocalDateTime getLastSeen(String userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        List<java.sql.Timestamp> results = namedParameterJdbcTemplate.queryForList(
                QueryConstants.USER_GET_LAST_SEEN, params, java.sql.Timestamp.class);
        if (results.isEmpty() || results.get(0) == null) return null;
        return results.get(0).toLocalDateTime();
    }

    public boolean updateProfilePic(String userId, String base64Image) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("profilePic", base64Image);
        int rows = namedParameterJdbcTemplate.update(QueryConstants.USER_UPDATE_PROFILE_PIC, params);
        return rows > 0;
    }

    public int updateFcmToken(String userId, String token, String platform) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("token", token);
        params.addValue("platform", platform);
        return namedParameterJdbcTemplate.update(QueryConstants.USER_UPDATE_FCM_TOKEN, params);
    }

    public int clearByDeadToken(String token) {
        MapSqlParameterSource params = new MapSqlParameterSource("token", token);
        return namedParameterJdbcTemplate.update(QueryConstants.USER_CLEAR_DEAD_FCM_TOKEN, params);
    }

    public User getUserByIdWithFcm(String userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        List<User> users = namedParameterJdbcTemplate.query(QueryConstants.USER_GET_BY_ID_WITH_FCM, params, new UserFcmRowMapper());
        return users.isEmpty() ? null : users.get(0);
    }

    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUserId(rs.getString("UserID"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setMobileno(rs.getString("mobileno"));
            user.setPublicKey(rs.getString("public_key"));
            user.setProfilePic(rs.getString("profile_pic")); // ensure profilePic is mapped
            return user;
        }
    }

    private static class UserCredentialsRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUserId(rs.getString("UserID"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setProfilePic(rs.getString("profile_pic"));
            user.setPublicKey(rs.getString("public_key"));
            return user;
        }
    }

    private static class UserLoginRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUserId(rs.getString("UserID"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setMobileno(rs.getString("mobileno"));
            user.setProfilePic(rs.getString("profile_pic"));
            user.setEthAddress(rs.getString("eth_address"));
            user.setPublicKey(rs.getString("public_key"));
            return user;
        }
    }

    private static class UserFcmRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUserId(rs.getString("UserID"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setMobileno(rs.getString("mobileno"));
            user.setProfilePic(rs.getString("profile_pic"));
            user.setEthAddress(rs.getString("eth_address"));
            user.setPublicKey(rs.getString("public_key"));
            user.setFcmToken(rs.getString("fcm_token"));
            user.setFcmPlatform(rs.getString("fcm_platform"));
            java.sql.Timestamp ts = rs.getTimestamp("fcm_token_updated_at");
            user.setFcmTokenUpdatedAt(ts != null ? ts.toLocalDateTime() : null);
            return user;
        }
    }
}
