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

/**
 * Data Access Object (DAO) for User operations
 */
@Repository
public class UserDao {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Fetch all users from the database
     * @return List of User objects
     */
    public List<User> getAllUsers() {
        return namedParameterJdbcTemplate.query(QueryConstants.USER_GET_ALL, new MapSqlParameterSource(), new UserRowMapper());
    }

    /**
     * Fetch user by email
     * @param email User's email
     * @return User object or null if not found
     */
    public User getUserByEmail(String email) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("email", email);
        List<User> users = namedParameterJdbcTemplate.query(QueryConstants.USER_GET_BY_EMAIL, parameters, new UserRowMapper());
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Fetch user by username
     * @param username User's username
     * @return User object or null if not found
     */
    public User getUserByUsername(String username) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("username", username);
        List<User> users = namedParameterJdbcTemplate.query(QueryConstants.USER_GET_BY_USERNAME, parameters, new UserRowMapper());
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Fetch user credentials for login validation
     * @param email User's email
     * @return User object with email, password, and profile_pic, or null if not found
     */
    public User getUserCredentials(String email) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("email", email);
        List<User> users = namedParameterJdbcTemplate.query(QueryConstants.USER_GET_CREDENTIALS, parameters, new UserCredentialsRowMapper());
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Get full user details for successful login response
     * @param email User's email
     * @return Complete User object with all details including profile_pic and UserID
     */
    public User getUserForLogin(String email) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("email", email);
        List<User> users = namedParameterJdbcTemplate.query(QueryConstants.USER_GET_FOR_LOGIN, parameters, new UserLoginRowMapper());
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Fetch user by userId
     * @param userId User's ID
     * @return User object or null if not found
     */
    public User getUserById(String userId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("userId", userId);
        List<User> users = namedParameterJdbcTemplate.query(QueryConstants.USER_GET_BY_ID, parameters, new UserLoginRowMapper());
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Insert a new user into the database (sign up)
     * @param user User object with details
     * @return true if inserted successfully, false otherwise
     */
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

    /**
     * Update password for a user (used for BCrypt migration)
     */
    public void updatePassword(String email, String hashedPassword) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("password", hashedPassword);
        params.addValue("email", email);
        namedParameterJdbcTemplate.update(QueryConstants.USER_UPDATE_PASSWORD, params);
    }

    /**
     * Update the public key for a user by email
     */
    public void updateUserPublicKey(String email, String publicKey) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("publicKey", publicKey);
        params.addValue("email", email);
        namedParameterJdbcTemplate.update(QueryConstants.USER_UPDATE_PUBLIC_KEY, params);
    }

    /**
     * Update last_seen timestamp for a user when they disconnect
     */
    public void updateLastSeen(String userId, java.time.LocalDateTime lastSeen) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("lastSeen", lastSeen);
        namedParameterJdbcTemplate.update(QueryConstants.USER_UPDATE_LAST_SEEN, params);
    }

    /**
     * Get last_seen timestamp for a user by userId
     */
    public java.time.LocalDateTime getLastSeen(String userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        List<java.sql.Timestamp> results = namedParameterJdbcTemplate.queryForList(
                QueryConstants.USER_GET_LAST_SEEN, params, java.sql.Timestamp.class);
        if (results.isEmpty() || results.get(0) == null) return null;
        return results.get(0).toLocalDateTime();
    }

    /**
     * Update profile_pic (base64) for a user by userId
     */
    public boolean updateProfilePic(String userId, String base64Image) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("profilePic", base64Image);
        int rows = namedParameterJdbcTemplate.update(QueryConstants.USER_UPDATE_PROFILE_PIC, params);
        return rows > 0;
    }

    /**
     * RowMapper to map database rows to User objects
     */
    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUserId(rs.getString("UserID"));  // Fixed: Use getString for String type
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setMobileno(rs.getString("mobileno"));
            user.setPublicKey(rs.getString("public_key")); // Added mapping for publicKey
            return user;
        }
    }

    /**
     * RowMapper specifically for login credentials
     */
    private static class UserCredentialsRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUserId(rs.getString("UserID"));  // Fixed: Use getString for String type
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setProfilePic(rs.getString("profile_pic"));
            user.setPublicKey(rs.getString("public_key")); // Added mapping for publicKey
            return user;
        }
    }

    /**
     * RowMapper for complete user details including profile_pic and UserID
     */
    private static class UserLoginRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUserId(rs.getString("UserID"));  // Fixed: Use getString for String type
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setMobileno(rs.getString("mobileno"));
            user.setProfilePic(rs.getString("profile_pic"));
            user.setEthAddress(rs.getString("eth_address"));
            user.setPublicKey(rs.getString("public_key")); // Added mapping for publicKey
            return user;
        }
    }
}
