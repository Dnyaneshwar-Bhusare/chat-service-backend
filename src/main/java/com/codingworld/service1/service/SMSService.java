package com.codingworld.service1.service;

import com.codingworld.service1.dao.OtpDao;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class SMSService {

    private static final Logger logger = LoggerFactory.getLogger(SMSService.class);

    @Value("${sms-api-key}")
    private String nexmoApiKey;

    @Value("${sms-api-secret}")
    private String nexmoApiSecret;

    private static final String NEXMO_FROM = "Secure Chat";
    private static final String NEXMO_URL  = "https://rest.nexmo.com/sms/json";

    @Autowired
    private OtpDao otpDao;

    /**
     * Generates a 6-digit OTP, saves it to DB, and sends it via Nexmo SMS.
     *
     * @param mobileNo  E.164 format mobile number e.g. "919325453006"
     * @return true if SMS was sent successfully, false otherwise
     */
    public boolean sendOtp(String mobileNo) {
        String otp = generateOtp();
        logger.info("[SMSService] Sending OTP to mobile: {}", mobileNo);

        try {
            OkHttpClient client = new OkHttpClient();

            String text = "This is for otp verification. Your OTP for Secure Chat is: " + otp;

            RequestBody formBody = new FormBody.Builder()
                    .add("api_key",    nexmoApiKey)
                    .add("api_secret", nexmoApiSecret)
                    .add("to",         mobileNo)
                    .add("from",       NEXMO_FROM)
                    .add("text",       text)
                    .build();

            Request request = new Request.Builder()
                    .url(NEXMO_URL)
                    .post(formBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                logger.info("[SMSService] Nexmo response for {}: status={}, body={}", mobileNo, response.code(), responseBody);

                if (response.isSuccessful()) {
                    // Save OTP to DB only after successful send
                    otpDao.saveOtpForMobile(mobileNo, otp);
                    logger.info("[SMSService] OTP saved to DB for mobile: {}", mobileNo);
                    return true;
                } else {
                    logger.error("[SMSService] Nexmo API error for mobile {}: {}", mobileNo, responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("[SMSService] Exception while sending OTP to {}: {}", mobileNo, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates OTP for the given mobile number using DB lookup.
     * Deletes OTP from DB after successful validation (one-time use).
     *
     * @param mobileNo mobile number
     * @param otp      OTP entered by user
     * @return true if OTP is valid, false otherwise
     */
    public boolean validateOtp(String mobileNo, String otp) {
        logger.info("[SMSService] Validating OTP for mobile: {}", mobileNo);
        boolean valid = otpDao.validateOtpForMobile(mobileNo, otp);
        if (valid) {
            // Delete OTP after successful validation (one-time use)
            otpDao.deleteOtpByMobile(mobileNo);
            logger.info("[SMSService] OTP validated and cleared for mobile: {}", mobileNo);
        } else {
            logger.warn("[SMSService] Invalid OTP attempt for mobile: {}", mobileNo);
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
