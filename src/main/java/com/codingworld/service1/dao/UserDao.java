package com.codingworld.service1.dao;

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
        String sql = "SELECT UserID, username, email, mobileno, public_key FROM db_chat.users where public_key is not null";

        return namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource(), new UserRowMapper());
    }

    /**
     * Fetch user by email
     * @param email User's email
     * @return User object or null if not found
     */
    public User getUserByEmail(String email) {
        String sql = "SELECT username, email, mobileno, public_key FROM db_chat.users WHERE email = :email";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("email", email);

        List<User> users = namedParameterJdbcTemplate.query(sql, parameters, new UserRowMapper());

        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Fetch user by username
     * @param username User's username
     * @return User object or null if not found
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT username, email, mobileno, public_key FROM db_chat.users WHERE username = :username";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("username", username);

        List<User> users = namedParameterJdbcTemplate.query(sql, parameters, new UserRowMapper());

        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Fetch user credentials for login validation
     * @param email User's email
     * @return User object with email, password, and profile_pic, or null if not found
     */
    public User getUserCredentials(String email) {
        String sql = "SELECT UserID, email, password, profile_pic, public_key FROM db_chat.users WHERE email = :email";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("email", email);

        List<User> users = namedParameterJdbcTemplate.query(sql, parameters, new UserCredentialsRowMapper());

        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Get full user details for successful login response
     * @param email User's email
     * @return Complete User object with all details including profile_pic and UserID
     */
    public User getUserForLogin(String email) {
        String sql = "SELECT UserID, username, email, mobileno, profile_pic, eth_address, public_key FROM db_chat.users WHERE email = :email";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("email", email);

        List<User> users = namedParameterJdbcTemplate.query(sql, parameters, new UserLoginRowMapper());

        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * Insert a new user into the database (sign up)
     * @param user User object with details
     * @return true if inserted successfully, false otherwise
     */
    public boolean insertUser(User user) {
        String sql = "INSERT INTO db_chat.users (username, email, password, profile_pic, mobileno, public_key) " +
                "VALUES (:username, :email, :password, :profilePic, :mobileno, :publicKey)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("username", user.getUsername());
        params.addValue("email", user.getEmail());
        params.addValue("password", user.getPassword());
        params.addValue("profilePic", user.getProfilePic());
        params.addValue("mobileno", user.getMobileno());
        params.addValue("publicKey", user.getPublicKey());
        int rows = namedParameterJdbcTemplate.update(sql, params);
        return rows > 0;
    }

    /**
     * Update password for a user (used for BCrypt migration)
     */
    public void updatePassword(String email, String hashedPassword) {
        String sql = "UPDATE db_chat.users SET password = :password WHERE email = :email";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("password", hashedPassword);
        params.addValue("email", email);
        namedParameterJdbcTemplate.update(sql, params);
    }

    /**
     * Update the public key for a user by email
     */
    public void updateUserPublicKey(String email, String publicKey) {
        String sql = "UPDATE db_chat.users SET public_key = :publicKey WHERE email = :email";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("publicKey", publicKey);
        params.addValue("email", email);
        namedParameterJdbcTemplate.update(sql, params);
    }

    /**
     * Fetch user by UserID — used to resolve ETH address for blockchain transactions
     * @param userId The numeric UserID (stored as String)
     * @return User object or null if not found
     */
    public User getUserById(String userId) {
        String sql = "SELECT UserID, username, email, mobileno, profile_pic, eth_address, public_key " +
                     "FROM db_chat.users WHERE UserID = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        List<User> users = namedParameterJdbcTemplate.query(sql, params, new UserLoginRowMapper());
        return users.isEmpty() ? null : users.get(0);
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
