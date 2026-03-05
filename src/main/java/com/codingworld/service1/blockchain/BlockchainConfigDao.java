package com.codingworld.service1.blockchain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DAO to persist blockchain config (contract address, wallet key) in DB.
 * This way nothing needs to be hardcoded or manually updated in properties.
 */
@Repository
public class BlockchainConfigDao {

    private static final String TABLE =
            "CREATE TABLE IF NOT EXISTS db_chat.blockchain_config (" +
            "  config_key   VARCHAR(100) PRIMARY KEY," +
            "  config_value VARCHAR(500) NOT NULL," +
            "  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ")";

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    /** Called once on startup to make sure the table exists */
    public void ensureTableExists() {
        jdbc.getJdbcTemplate().execute(TABLE);
    }

    public String get(String key) {
        String sql = "SELECT config_value FROM db_chat.blockchain_config WHERE config_key = :key";
        List<String> results = jdbc.queryForList(sql,
                new MapSqlParameterSource("key", key), String.class);
        return results.isEmpty() ? null : results.get(0);
    }

    public void set(String key, String value) {
        String sql = "INSERT INTO db_chat.blockchain_config (config_key, config_value) " +
                     "VALUES (:key, :value) " +
                     "ON DUPLICATE KEY UPDATE config_value = :value";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("key", key)
                .addValue("value", value);
        jdbc.update(sql, params);
    }

    public void delete(String key) {
        String sql = "DELETE FROM db_chat.blockchain_config WHERE config_key = :key";
        jdbc.update(sql, new MapSqlParameterSource("key", key));
    }
}
