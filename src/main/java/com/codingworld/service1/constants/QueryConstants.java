package com.codingworld.service1.constants;

public class QueryConstants {

    private QueryConstants() {
        throw new AssertionError();
    }

    public static final String USER_GET_ALL =
            "SELECT UserID, username, email, mobileno, public_key, profile_pic " +
            "FROM  users " +
            "WHERE public_key IS NOT NULL";

    public static final String USER_GET_BY_EMAIL =
            "SELECT username, email, mobileno, public_key, profile_pic " +
            "FROM  users " +
            "WHERE email = :email";

    public static final String USER_GET_BY_USERNAME =
            "SELECT username, email, mobileno, public_key, profile_pic " +
            "FROM  users " +
            "WHERE username = :username";

    public static final String USER_GET_CREDENTIALS =
            "SELECT UserID, email, password, profile_pic, public_key " +
            "FROM  users " +
            "WHERE email = :email";

    public static final String USER_GET_FOR_LOGIN =
            "SELECT UserID, username, email, mobileno, profile_pic, eth_address, public_key " +
            "FROM  users " +
            "WHERE email = :email";

    public static final String USER_GET_BY_ID =
            "SELECT UserID, username, email, mobileno, profile_pic, eth_address, public_key " +
            "FROM  users " +
            "WHERE UserID = :userId";

    public static final String USER_INSERT =
            "INSERT INTO  users (username, email, password, profile_pic, mobileno, public_key) " +
            "VALUES (:username, :email, :password, :profilePic, :mobileno, :publicKey)";

    public static final String USER_UPDATE_PASSWORD =
            "UPDATE  users SET password = :password WHERE email = :email";

    public static final String USER_UPDATE_PUBLIC_KEY =
            "UPDATE  users SET public_key = :publicKey WHERE email = :email";

    public static final String USER_UPDATE_LAST_SEEN =
            "UPDATE  users SET last_seen = :lastSeen WHERE UserID = :userId";

    public static final String USER_GET_LAST_SEEN =
            "SELECT last_seen FROM  users WHERE UserID = :userId";

    public static final String USER_UPDATE_PROFILE_PIC =
            "UPDATE  users SET profile_pic = :profilePic WHERE UserID = :userId";

    public static final String CHAT_INSERT =
            "INSERT INTO  chat_table " +
            "(chat_id, message, message_1, `from`, `to`, algo, tx_hash, created_ts) " +
            "VALUES (:chatId, :message, :messageToSelf, :fromUser, :toUser, :algo, :txHash, :createdTs)";

    public static final String CHAT_GET_ALL_FOR_USER = """
            SELECT c.chat_id,
                   CASE
                       WHEN c.`from` = :userId THEN c.`message_1`
                       ELSE c.`message`
                   END AS message_for_sender,
                   c.`from`, c.`to`, c.algo, c.created_ts, c.tx_hash,
                   u.public_key
            FROM  chat_table c
            LEFT JOIN  users u ON c.`from` = u.UserID
            WHERE c.`to` = :userId OR c.`from` = :userId
            ORDER BY c.created_ts DESC
            """;

    public static final String CHAT_GET_BETWEEN_USERS = """
            SELECT c.chat_id,
                   CASE
                       WHEN c.`from` = :sender THEN c.`message_1`
                       WHEN c.`to` = :sender THEN c.`message`
                   END AS message_for_sender,
                   c.`from`, c.`to`, c.algo, c.created_ts, c.tx_hash,
                   u.public_key
            FROM  chat_table c
            LEFT JOIN  users u ON c.`from` = u.UserID
            WHERE (c.`from` = :sender AND c.`to` = :reciver)
               OR (c.`from` = :reciver AND c.`to` = :sender)
            ORDER BY c.created_ts DESC
            """;

    public static final String CHAT_GET_NEW_FOR_USER =
            "SELECT chat_id, message, `from`, `to`, algo, created_ts, tx_hash " +
            "FROM  chat_table " +
            "WHERE (`to` = :userId OR `from` = :userId) " +
            "AND created_ts > :afterTime " +
            "ORDER BY created_ts ASC";

    public static final String CHAT_GET_BY_ID =
            "SELECT c.chat_id, c.message AS message_for_sender, " +
            "c.`from`, c.`to`, c.algo, c.created_ts, c.tx_hash, " +
            "u.public_key " +
            "FROM  chat_table c " +
            "LEFT JOIN  users u ON c.`from` = u.UserID " +
            "WHERE c.chat_id = :chatId";

    public static final String CHAT_DELETE_BY_ID =
            "DELETE FROM  chat_table WHERE chat_id = :chatId";

    public static final String CHAT_DELETE_CONVERSATION =
            "DELETE FROM  chat_table " +
            "WHERE (`from` = :userId1 AND `to` = :userId2) " +
            "OR    (`from` = :userId2 AND `to` = :userId1)";

    public static final String CHAT_DELETE_ALL_FOR_USER =
            "DELETE FROM  chat_table " +
            "WHERE `from` = :userId OR `to` = :userId";

    public static final String NOTIFICATION_INSERT =
            "INSERT INTO notification_table (notification_type, notification_ref_id, timestamp) " +
            "VALUES (:type, :refId, :timestamp)";

    public static final String NOTIFICATION_GET_ALL =
            "SELECT notification_type, notification_ref_id, timestamp FROM notification_table";

    public static final String NOTIFICATION_GET_BY_TYPE_AND_REF = """
            SELECT notification_type, notification_ref_id, timestamp
            FROM notification_table
            WHERE notification_type = 'LOGIN'
              AND notification_ref_id = :refId
            UNION ALL
            SELECT notification_type, notification_ref_id, timestamp
            FROM notification_table n
            INNER JOIN chat_table c ON c.chat_id = n.notification_ref_id
            WHERE notification_type = 'DECRYPT_FAILURE'
              AND (c.`from` = :refId OR c.`to` = :refId)
            """;

    public static final String BLOCKCHAIN_CONFIG_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS  blockchain_config (" +
            "  config_key   VARCHAR(100) PRIMARY KEY," +
            "  config_value VARCHAR(500) NOT NULL," +
            "  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ")";

    public static final String BLOCKCHAIN_CONFIG_GET =
            "SELECT config_value FROM  blockchain_config WHERE config_key = :key";

    public static final String BLOCKCHAIN_CONFIG_SET =
            "INSERT INTO  blockchain_config (config_key, config_value) " +
            "VALUES (:key, :value) " +
            "ON DUPLICATE KEY UPDATE config_value = :value";

    public static final String BLOCKCHAIN_CONFIG_DELETE =
            "DELETE FROM  blockchain_config WHERE config_key = :key";

    // ── OTP ──────────────────────────────────────────────────────────────────
    // NOTE: Table must have UNIQUE KEY on email and UNIQUE KEY on mobile_no
    // ALTER TABLE otp_valadator ADD UNIQUE KEY uq_email (email);
    // ALTER TABLE otp_valadator ADD UNIQUE KEY uq_mobile (mobile_no);

    public static final String OTP_UPSERT_BY_EMAIL =
            "INSERT INTO otp_valadator (email, mobile_no, otp, created_ts) " +
            "VALUES (:email, NULL, :otp, :createdTs) " +
            "ON DUPLICATE KEY UPDATE otp = :otp, created_ts = :createdTs";

    public static final String OTP_UPSERT_BY_MOBILE =
            "INSERT INTO otp_valadator (mobile_no, email, otp, created_ts) " +
            "VALUES (:mobileNo, NULL, :otp, :createdTs) " +
            "ON DUPLICATE KEY UPDATE otp = :otp, created_ts = :createdTs";

    public static final String OTP_VALIDATE_BY_MOBILE =
            "SELECT COUNT(*) FROM otp_valadator " +
            "WHERE mobile_no = :mobileNo AND otp = :otp " +
            "ORDER BY created_ts DESC LIMIT 1";

    public static final String OTP_VALIDATE_BY_EMAIL =
            "SELECT COUNT(*) FROM otp_valadator " +
            "WHERE email = :email AND otp = :otp " +
            "ORDER BY created_ts DESC LIMIT 1";

    public static final String OTP_DELETE_BY_MOBILE =
            "DELETE FROM otp_valadator WHERE mobile_no = :mobileNo";

    public static final String OTP_DELETE_BY_EMAIL =
            "DELETE FROM otp_valadator WHERE email = :email";

    // ── FCM Token ────────────────────────────────────────────────────────────
    public static final String USER_UPDATE_FCM_TOKEN =
            "UPDATE users SET fcm_token = :token, fcm_platform = :platform, fcm_token_updated_at = NOW() WHERE UserID = :userId";

    public static final String USER_CLEAR_DEAD_FCM_TOKEN =
            "UPDATE users SET fcm_token = NULL, fcm_platform = NULL, fcm_token_updated_at = NOW() WHERE fcm_token = :token";

    public static final String USER_GET_BY_ID_WITH_FCM =
            "SELECT UserID, username, email, mobileno, profile_pic, eth_address, public_key, fcm_token, fcm_platform, fcm_token_updated_at " +
            "FROM users WHERE UserID = :userId";

    public static final String CHAT_UPDATE_MESSAGE_BY_ID =
        "UPDATE chat_table SET message = :message, " +
        "message_1 = COALESCE(:messageToSelf, message_1), " +
        "edited = true " +
        "WHERE chat_id = :chatId";
}
