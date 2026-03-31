package com.codingworld.service1.controller;

import com.codingworld.service1.service.EmailAndMobileValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/validate")
public class EmailAndMobileValidatorController {

    private static final Logger logger = LoggerFactory.getLogger(EmailAndMobileValidatorController.class);

    @Autowired
    private EmailAndMobileValidatorService validatorService;

    /**
     * POST /validate/email/send-otp
     * Sends an OTP to the given email address via Brevo API.
     * Body params: email, name
     */
    @PostMapping("/email/send-otp")
    public ResponseEntity<Map<String, Object>> sendEmailOtp(
            @RequestParam String email,
            @RequestParam(defaultValue = "User") String name) {

        logger.info("[ValidatorController] POST /validate/email/send-otp -> email={}", email);
        Map<String, Object> response = new HashMap<>();

        boolean sent = validatorService.sendEmailOtp(email, name);
        response.put("email", email);
        response.put("sent", sent);
        response.put("message", sent ? "OTP sent successfully to " + email : "Failed to send OTP");

        logger.info("[ValidatorController] send-email-otp response: {}", response);
        return sent ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().body(response);
    }

    /**
     * POST /validate/email/verify-otp
     * Validates OTP for the given email. One-time use — OTP is deleted after success.
     * Body params: email, otp
     */
    @PostMapping("/email/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyEmailOtp(
            @RequestParam String email,
            @RequestParam String otp) {

        logger.info("[ValidatorController] POST /validate/email/verify-otp -> email={}", email);
        Map<String, Object> response = new HashMap<>();

        boolean valid = validatorService.validateEmailOtp(email, otp);
        response.put("email", email);
        response.put("valid", valid);
        response.put("message", valid ? "OTP verified successfully" : "Invalid or expired OTP");

        logger.info("[ValidatorController] verify-email-otp response: {}", response);
        return valid ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * POST /validate/mobile/send-otp
     * Sends an OTP to the given mobile number via Nexmo SMS API.
     * Body params: mobileNo (E.164 format, e.g. 919325453006)
     */
    @PostMapping("/mobile/send-otp")
    public ResponseEntity<Map<String, Object>> sendMobileOtp(@RequestParam String mobileNo) {
        logger.info("[ValidatorController] POST /validate/mobile/send-otp -> mobileNo={}", mobileNo);
        Map<String, Object> response = new HashMap<>();

        boolean sent = validatorService.sendMobileOtp(mobileNo);
        response.put("mobileNo", mobileNo);
        response.put("sent", sent);
        response.put("message", sent ? "OTP sent successfully to " + mobileNo : "Failed to send OTP");

        logger.info("[ValidatorController] send-mobile-otp response: {}", response);
        return sent ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().body(response);
    }

    /**
     * POST /validate/mobile/verify-otp
     * Validates OTP for the given mobile number. One-time use — OTP is deleted after success.
     * Body params: mobileNo, otp
     */
    @PostMapping("/mobile/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyMobileOtp(
            @RequestParam String mobileNo,
            @RequestParam String otp) {

        logger.info("[ValidatorController] POST /validate/mobile/verify-otp -> mobileNo={}", mobileNo);
        Map<String, Object> response = new HashMap<>();

        boolean valid = validatorService.validateMobileOtp(mobileNo, otp);
        response.put("mobileNo", mobileNo);
        response.put("valid", valid);
        response.put("message", valid ? "OTP verified successfully" : "Invalid or expired OTP");

        logger.info("[ValidatorController] verify-mobile-otp response: {}", response);
        return valid ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }


}
