package com.codingworld.service1.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoHelper {
    private static final String SECRET_KEY = "Bncoe@2025Vm@#9NzK&FdBwY6*MhC0Qx".substring(0, 16); // Ensure 16-byte AES key
    private static final String AES_ALGO = "AES/CBC/PKCS5Padding"; // Compatible with Dart PKCS7Padding
    private static final SecureRandom SECURE_RANDOM = new SecureRandom(); // ✅ Cryptographically secure random

    // 🔹 Encrypt a message
    public static String encrypt(String message, String algo) {
        switch (algo.toLowerCase()) {
            case "aes":
                return encryptAES(message);
            case "base64":
                return encryptBase64(message);
            case "xor":
                return encryptXOR(message);
            default:
                return message;
        }
    }

    // 🔹 Decrypt a message
    public static String decrypt(String encryptedMessage, String algo) {
        switch (algo.toLowerCase()) {
            case "aes":
                return decryptAES(encryptedMessage);
            case "base64":
                return decryptBase64(encryptedMessage);
            case "xor":
                return decryptXOR(encryptedMessage);
            default:
                return encryptedMessage;
        }
    }

    // 🔹 AES Encryption — random IV every time
    private static String encryptAES(String message) {
        try {
            byte[] iv = new byte[16];
            SECURE_RANDOM.nextBytes(iv); // ✅ Random IV per message, not all-zeros
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(message.getBytes());
            // Format: base64(iv):base64(ciphertext)
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            return "AES Encryption Error: " + e.getMessage();
        }
    }

    // 🔹 AES Decryption — extracts IV from the cipher text prefix
    public static String decryptAES(String cipherText) {
        try {
            // Split IV and encrypted message
            String[] parts = cipherText.split(":");
            if (parts.length != 2) {
                return "Invalid cipher text format";
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]); // Extract IV
            byte[] encryptedBytes = Base64.getDecoder().decode(parts[1]); // Extract encrypted text

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes);
        } catch (Exception e) {
            return "AES Decryption Error: " + e.getMessage();
        }
    }

    // 🔹 Base64 Encryption
    private static String encryptBase64(String message) {
        return Base64.getEncoder().encodeToString((SECRET_KEY + message).getBytes());
    }

    // 🔹 Base64 Decryption
    private static String decryptBase64(String encryptedMessage) {
        String decodedMessage = new String(Base64.getDecoder().decode(encryptedMessage));
        return decodedMessage.replaceFirst(SECRET_KEY, ""); // Remove key prefix
    }

    // 🔹 XOR Encryption
    private static String encryptXOR(String message) {
        byte[] messageBytes = message.getBytes();
        byte[] keyBytes = SECRET_KEY.getBytes();

        byte[] encryptedBytes = new byte[messageBytes.length];
        for (int i = 0; i < messageBytes.length; i++) {
            encryptedBytes[i] = (byte) (messageBytes[i] ^ keyBytes[i % keyBytes.length]);
        }

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // 🔹 XOR Decryption
    private static String decryptXOR(String encryptedMessage) {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedMessage);
        byte[] keyBytes = SECRET_KEY.getBytes();

        byte[] decryptedBytes = new byte[encryptedBytes.length];
        for (int i = 0; i < encryptedBytes.length; i++) {
            decryptedBytes[i] = (byte) (encryptedBytes[i] ^ keyBytes[i % keyBytes.length]);
        }

        return new String(decryptedBytes);
    }
}
