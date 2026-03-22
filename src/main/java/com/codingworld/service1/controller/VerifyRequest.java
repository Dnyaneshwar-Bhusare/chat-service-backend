package com.codingworld.service1.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Minimal request body for the /verifyMessage endpoint.
 * Only the raw message content is needed to recompute the keccak256 hash
 * and look it up on-chain; from/to/algo fields are irrelevant for verification.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyRequest {
    private String message;
}

