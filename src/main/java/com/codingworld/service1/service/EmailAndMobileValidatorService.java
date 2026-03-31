package com.codingworld.service1.service;

import com.codingworld.service1.dao.UserValidationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailAndMobileValidatorService {

    private static final Logger logger = LoggerFactory.getLogger(EmailAndMobileValidatorService.class);

    @Autowired
    private UserValidationDao userValidationDao;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SMSService smsService;




    /**
     * Sends an OTP to the given email address via Brevo API.
     *
     * @param email  recipient email
     * @param name   recipient display name (used in email body)
     * @return true if OTP was sent successfully
     */
    public boolean sendEmailOtp(String email, String name) {
        logger.info("[ValidatorService] Sending email OTP to: {}", email);
        return emailService.sendOtp(email, name);
    }

    /**
     * Validates the OTP entered by the user for a given email.
     * OTP is deleted from DB on successful validation (one-time use).
     *
     * @param email recipient email
     * @param otp   OTP entered by user
     * @return true if OTP is valid
     */
    public boolean validateEmailOtp(String email, String otp) {
        logger.info("[ValidatorService] Validating email OTP for: {}", email);
        return emailService.validateOtp(email, otp);
    }

    /**
     * Sends an OTP to the given mobile number via Nexmo SMS API.
     *
     * @param mobileNo E.164 format mobile number e.g. "919325453006"
     * @return true if OTP was sent successfully
     */
    public boolean sendMobileOtp(String mobileNo) {
        logger.info("[ValidatorService] Sending SMS OTP to: {}", mobileNo);
        return smsService.sendOtp(mobileNo);
    }

    /**
     * Validates the OTP entered by the user for a given mobile number.
     * OTP is deleted from DB on successful validation (one-time use).
     *
     * @param mobileNo mobile number
     * @param otp      OTP entered by user
     * @return true if OTP is valid
     */
    public boolean validateMobileOtp(String mobileNo, String otp) {
        logger.info("[ValidatorService] Validating SMS OTP for: {}", mobileNo);
        return smsService.validateOtp(mobileNo, otp);
    }
}

