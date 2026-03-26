package com.codingworld.service1.blockchain;

import com.codingworld.service1.constants.QueryConstants;
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

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    /** Called once on startup to make sure the table exists */
    public void ensureTableExists() {
        jdbc.getJdbcTemplate().execute(QueryConstants.BLOCKCHAIN_CONFIG_CREATE_TABLE);
    }

    public String get(String key) {
        List<String> results = jdbc.queryForList(QueryConstants.BLOCKCHAIN_CONFIG_GET,
                new MapSqlParameterSource("key", key), String.class);
        return results.isEmpty() ? null : results.get(0);
    }

    public void set(String key, String value) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("key", key)
                .addValue("value", value);
        jdbc.update(QueryConstants.BLOCKCHAIN_CONFIG_SET, params);
    }

    public void delete(String key) {
        jdbc.update(QueryConstants.BLOCKCHAIN_CONFIG_DELETE, new MapSqlParameterSource("key", key));
    }
}
