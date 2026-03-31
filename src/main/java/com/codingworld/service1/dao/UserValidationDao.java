package com.codingworld.service1.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserValidationDao {

    private static final Logger logger = LoggerFactory.getLogger(UserValidationDao.class);

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = :email";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("email", email);
        Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
        boolean exists = count != null && count > 0;
        logger.info("[UserValidationDao] Email {} exists: {}", email, exists);
        return exists;
    }

    public boolean existsByMobile(String mobile) {
        String sql = "SELECT COUNT(*) FROM users WHERE mobileno = :mobile";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("mobile", mobile);
        Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
        boolean exists = count != null && count > 0;
        logger.info("[UserValidationDao] Mobile {} exists: {}", mobile, exists);
        return exists;
    }
}
