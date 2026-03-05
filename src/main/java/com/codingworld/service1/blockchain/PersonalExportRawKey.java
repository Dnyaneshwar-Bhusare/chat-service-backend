package com.codingworld.service1.blockchain;

import org.web3j.protocol.core.Response;

/**
 * Web3j response wrapper for personal_exportRawKey RPC call.
 * Ganache exposes this to get the private key of any managed account.
 */
public class PersonalExportRawKey extends Response<String> {

    /**
     * Returns the raw private key hex string for the requested account.
     */
    public String getPrivateKey() {
        return getResult();
    }
}

