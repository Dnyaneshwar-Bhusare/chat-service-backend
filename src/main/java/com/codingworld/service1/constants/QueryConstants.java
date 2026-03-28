package com.codingworld.service1.constants;

public class QueryConstants {

    private QueryConstants() {
        throw new AssertionError();
    }

    public static final String USER_GET_ALL =
            "SELECT UserID, username, email, mobileno, public_key, profile_pic " +
            "FROM db_chat.users " +
            "WHERE public_key IS NOT NULL";

    public static final String USER_GET_BY_EMAIL =
            "SELECT username, email, mobileno, public_key, profile_pic " +
            "FROM db_chat.users " +
            "WHERE email = :email";

    public static final String USER_GET_BY_USERNAME =
            "SELECT username, email, mobileno, public_key, profile_pic " +
            "FROM db_chat.users " +
            "WHERE username = :username";

    public static final String USER_GET_CREDENTIALS =
            "SELECT UserID, email, password, profile_pic, public_key " +
            "FROM db_chat.users " +
            "WHERE email = :email";

    public static final String USER_GET_FOR_LOGIN =
            "SELECT UserID, username, email, mobileno, profile_pic, eth_address, public_key " +
            "FROM db_chat.users " +
            "WHERE email = :email";

    public static final String USER_GET_BY_ID =
            "SELECT UserID, username, email, mobileno, profile_pic, eth_address, public_key " +
            "FROM db_chat.users " +
            "WHERE UserID = :userId";

    public static final String USER_INSERT =
            "INSERT INTO db_chat.users (username, email, password, profile_pic, mobileno, public_key) " +
            "VALUES (:username, :email, :password, :profilePic, :mobileno, :publicKey)";

    public static final String USER_UPDATE_PASSWORD =
            "UPDATE db_chat.users SET password = :password WHERE email = :email";

    public static final String USER_UPDATE_PUBLIC_KEY =
            "UPDATE db_chat.users SET public_key = :publicKey WHERE email = :email";

    public static final String USER_UPDATE_LAST_SEEN =
            "UPDATE db_chat.users SET last_seen = :lastSeen WHERE UserID = :userId";

    public static final String USER_GET_LAST_SEEN =
            "SELECT last_seen FROM db_chat.users WHERE UserID = :userId";

    public static final String USER_UPDATE_PROFILE_PIC =
            "UPDATE db_chat.users SET profile_pic = :profilePic WHERE UserID = :userId";

    public static final String CHAT_INSERT =
            "INSERT INTO db_chat.chat_table " +
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
            FROM db_chat.chat_table c
            LEFT JOIN db_chat.users u ON c.`from` = u.UserID
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
            FROM db_chat.chat_table c
            LEFT JOIN db_chat.users u ON c.`from` = u.UserID
            WHERE (c.`from` = :sender AND c.`to` = :reciver)
               OR (c.`from` = :reciver AND c.`to` = :sender)
            ORDER BY c.created_ts DESC
            """;

    public static final String CHAT_GET_NEW_FOR_USER =
            "SELECT chat_id, message, `from`, `to`, algo, created_ts, tx_hash " +
            "FROM db_chat.chat_table " +
            "WHERE (`to` = :userId OR `from` = :userId) " +
            "AND created_ts > :afterTime " +
            "ORDER BY created_ts ASC";

    public static final String CHAT_GET_BY_ID =
            "SELECT c.chat_id, c.message AS message_for_sender, " +
            "c.`from`, c.`to`, c.algo, c.created_ts, c.tx_hash, " +
            "u.public_key " +
            "FROM db_chat.chat_table c " +
            "LEFT JOIN db_chat.users u ON c.`from` = u.UserID " +
            "WHERE c.chat_id = :chatId";

    public static final String CHAT_DELETE_BY_ID =
            "DELETE FROM db_chat.chat_table WHERE chat_id = :chatId";

    public static final String CHAT_DELETE_CONVERSATION =
            "DELETE FROM db_chat.chat_table " +
            "WHERE (`from` = :userId1 AND `to` = :userId2) " +
            "OR    (`from` = :userId2 AND `to` = :userId1)";

    public static final String CHAT_DELETE_ALL_FOR_USER =
            "DELETE FROM db_chat.chat_table " +
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
            "CREATE TABLE IF NOT EXISTS db_chat.blockchain_config (" +
            "  config_key   VARCHAR(100) PRIMARY KEY," +
            "  config_value VARCHAR(500) NOT NULL," +
            "  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ")";

    public static final String BLOCKCHAIN_CONFIG_GET =
            "SELECT config_value FROM db_chat.blockchain_config WHERE config_key = :key";

    public static final String BLOCKCHAIN_CONFIG_SET =
            "INSERT INTO db_chat.blockchain_config (config_key, config_value) " +
            "VALUES (:key, :value) " +
            "ON DUPLICATE KEY UPDATE config_value = :value";

    public static final String BLOCKCHAIN_CONFIG_DELETE =
            "DELETE FROM db_chat.blockchain_config WHERE config_key = :key";
}
