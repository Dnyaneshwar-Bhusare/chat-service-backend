package com.codingworld.service1.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Database configuration class for NamedParameterJdbcTemplate bean
 */
@Configuration
public class DatabaseConfig {

    @Autowired
    private DataSource dataSource;

    /**
     * Creates and configures NamedParameterJdbcTemplate bean
     * This bean will be available for dependency injection throughout the application
     *
     * @return NamedParameterJdbcTemplate instance configured with the application's DataSource
     */
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * ObjectMapper bean for JSON serialization/deserialization
     * Configured to handle Java 8 date/time types
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
