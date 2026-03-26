package com.codingworld.service1.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Request body for the /verifyMessage endpoint.
 * Only the raw message content is needed to recompute the keccak256 hash
 * and look it up on-chain.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyRequest {
    private String message;
}

