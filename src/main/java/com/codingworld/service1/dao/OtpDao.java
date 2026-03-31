package com.codingworld.service1.dao;

import com.codingworld.service1.constants.QueryConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Repository
public class OtpDao {

    private static final Logger logger = LoggerFactory.getLogger(OtpDao.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Saves or UPDATES OTP for a mobile number using a single upsert query.
     * Requires UNIQUE KEY on mobile_no column in otp_valadator table.
     */
    public void saveOtpForMobile(String mobileNo, String otp) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("mobileNo", mobileNo);
        params.addValue("otp", otp);
        params.addValue("createdTs", LocalDateTime.now().format(FORMATTER));

        namedParameterJdbcTemplate.update(QueryConstants.OTP_UPSERT_BY_MOBILE, params);
        logger.info("[OtpDao] OTP upserted for mobile: {}", mobileNo);
    }

    /**
     * Saves or UPDATES OTP for an email using a single upsert query.
     * Requires UNIQUE KEY on email column in otp_valadator table.
     */
    public void saveOtpForEmail(String email, String otp) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("email", email);
        params.addValue("otp", otp);
        params.addValue("createdTs", LocalDateTime.now().format(FORMATTER));

        namedParameterJdbcTemplate.update(QueryConstants.OTP_UPSERT_BY_EMAIL, params);
        logger.info("[OtpDao] OTP upserted for email: {}", email);
    }

    /**
     * Validates OTP for a mobile number. Returns true if OTP matches.
     */
    public boolean validateOtpForMobile(String mobileNo, String otp) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("mobileNo", mobileNo);
        params.addValue("otp", otp);

        Integer count = namedParameterJdbcTemplate.queryForObject(
                QueryConstants.OTP_VALIDATE_BY_MOBILE, params, Integer.class);
        boolean valid = count != null && count > 0;
        logger.info("[OtpDao] OTP validation for mobile {}: {}", mobileNo, valid ? "VALID" : "INVALID");
        return valid;
    }

    /**
     * Validates OTP for an email address. Returns true if OTP matches.
     */
    public boolean validateOtpForEmail(String email, String otp) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("email", email);
        params.addValue("otp", otp);

        Integer count = namedParameterJdbcTemplate.queryForObject(
                QueryConstants.OTP_VALIDATE_BY_EMAIL, params, Integer.class);
        boolean valid = count != null && count > 0;
        logger.info("[OtpDao] OTP validation for email {}: {}", email, valid ? "VALID" : "INVALID");
        return valid;
    }

    /**
     * Deletes all OTPs for a mobile number (call after successful validation)
     */
    public void deleteOtpByMobile(String mobileNo) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("mobileNo", mobileNo);
        namedParameterJdbcTemplate.update(QueryConstants.OTP_DELETE_BY_MOBILE, params);
    }

    /**
     * Deletes all OTPs for an email (call after successful validation)
     */
    public void deleteOtpByEmail(String email) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("email", email);
        namedParameterJdbcTemplate.update(QueryConstants.OTP_DELETE_BY_EMAIL, params);
    }
}
