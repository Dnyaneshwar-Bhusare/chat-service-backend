package com.codingworld.service1.service;

import com.codingworld.service1.dao.OtpDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${email-api-key}")
    private String brevoApiKey;

    private static final String BREVO_URL    = "https://api.brevo.com/v3/smtp/email";
    private static final String SENDER_EMAIL = "sahildakhore2003@gmail.com";
    private static final String SENDER_NAME  = "Secure Chat";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OtpDao otpDao;

    /**
     * Generates a 6-digit OTP, saves it to DB, and sends it via Brevo (SendinBlue) email API.
     *
     * @param toEmail   Recipient email address
     * @param toName    Recipient display name
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendOtp(String toEmail, String toName) {
        String otp = generateOtp();
        logger.info("[EmailService] Sending OTP to email: {}", toEmail);

        try {
            OkHttpClient client = new OkHttpClient();

            String htmlContent = "<p>Hello " + toName + ", your one time password for Secure Chat is <b>" + otp + "</b>. "
                    + "Do not share it with anyone.</p>";

            // Build JSON payload safely using ObjectMapper — avoids broken JSON if
            // toName/toEmail contain quotes, backslashes, or other special characters
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", SENDER_NAME, "email", SENDER_EMAIL),
                    "to", List.of(Map.of("email", toEmail, "name", toName)),
                    "subject", "Email Verification | Secure Chat",
                    "htmlContent", htmlContent
            );

            String jsonBody = objectMapper.writeValueAsString(payload);
            logger.info("[EmailService] Request payload for {}: {}", toEmail, jsonBody);

            RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA);

            Request request = new Request.Builder()
                    .url(BREVO_URL)
                    .post(body)
                    .addHeader("api-key", brevoApiKey)
                    .addHeader("accept", "application/json")
                    .addHeader("content-type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                logger.info("[EmailService] Brevo response for {}: status={}, body={}", toEmail, response.code(), responseBody);

                if (response.isSuccessful()) {
                    // Save OTP to DB only after successful send
                    otpDao.saveOtpForEmail(toEmail, otp);
                    logger.info("[EmailService] OTP saved to DB for email: {}", toEmail);
                    return true;
                } else {
                    logger.error("[EmailService] Brevo API error for email {}: {}", toEmail, responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("[EmailService] Exception while sending OTP to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates OTP for the given email using DB lookup.
     * Deletes OTP from DB after successful validation (one-time use).
     *
     * @param email email address
     * @param otp   OTP entered by user
     * @return true if OTP is valid, false otherwise
     */
    public boolean validateOtp(String email, String otp) {
        logger.info("[EmailService] Validating OTP for email: {}", email);
        boolean valid = otpDao.validateOtpForEmail(email, otp);
        if (valid) {
            // Delete OTP after successful validation (one-time use)
            otpDao.deleteOtpByEmail(email);
            logger.info("[EmailService] OTP validated and cleared for email: {}", email);
        } else {
            logger.warn("[EmailService] Invalid OTP attempt for email: {}", email);
        }
        return valid;
    }

    /**
     * Generates a random 6-digit OTP.
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
