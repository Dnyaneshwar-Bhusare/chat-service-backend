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
        String sql = "SELECT UserID,username, email, mobileno FROM db_chat.users";

        return namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource(), new UserRowMapper());
    }

    /**
     * Fetch user by email
     * @param email User's email
     * @return User object or null if not found
     */
    public User getUserByEmail(String email) {
        String sql = "SELECT username, email, mobileno FROM db_chat.users WHERE email = :email";

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
        String sql = "SELECT username, email, mobileno FROM db_chat.users WHERE username = :username";

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
        String sql = "SELECT UserID, email, password, profile_pic FROM db_chat.users WHERE email = :email";

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
        String sql = "SELECT UserID, username, email, mobileno, profile_pic FROM db_chat.users WHERE email = :email";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("email", email);

        List<User> users = namedParameterJdbcTemplate.query(sql, parameters, new UserLoginRowMapper());

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
            return user;
        }
    }
}
