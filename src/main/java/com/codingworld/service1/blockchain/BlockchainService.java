package com.codingworld.service1.blockchain;


import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;

public class BlockchainService {
    // Configuration - SHOULD BE MOVED TO APPLICATION PROPERTIES!
    private static final String INFURA_URL = "https://polygon-mainnet.infura.io/v3/api_key_";
    private static final String CONTRACT_ADDRESS = "address_key_0*_from_remex";

    // Network configuration
    private static final long CHAIN_ID = 137L; // Polygon Mumbai Testnet
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(30_000_000_000L); // 30 Gwei
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(1_000_000L);

    private final Web3j web3j;
    private final ChatVerification contract;

    public BlockchainService() {
        // Validate configuration first
/*        if (INFURA_URL.contains("mainnet")) {







            throw new IllegalStateException("Mainnet detected! Should use testnet for development");
        }*/

        // Initialize with proper security
        this.web3j = Web3j.build(new HttpService(INFURA_URL));

        // Use environment variables for sensitive data
        String privateKey = "api_key_";
        if (privateKey == null || privateKey.isEmpty()) {
            throw new IllegalStateException("Private key not configured in environment variables");
        }

        Credentials credentials = Credentials.create(privateKey);
        ContractGasProvider gasProvider = new StaticGasProvider(GAS_PRICE, GAS_LIMIT);

        // EIP-155 compliant transaction manager
        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, CHAIN_ID);

        this.contract = ChatVerification.load(
                CONTRACT_ADDRESS,
                web3j,
                txManager,
                gasProvider
        );
    }

    public String storeMessageHash(String message, String receiverAddress) throws Exception {
        // Validate inputs
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        if (!isValidEthAddress(receiverAddress)) {
            throw new IllegalArgumentException("Invalid Ethereum address: " + receiverAddress);
        }

        try {
            // Generate hash
            String hash = org.web3j.crypto.Hash.sha3(message);
            byte[] hashBytes = Numeric.hexStringToByteArray(hash);

            // Ensure proper bytes32 length
            if (hashBytes.length > 32) {
                hashBytes = Arrays.copyOfRange(hashBytes, 0, 32);
            } else if (hashBytes.length < 32) {
                byte[] padded = new byte[32];
                System.arraycopy(hashBytes, 0, padded, 32 - hashBytes.length, hashBytes.length);
                hashBytes = padded;
            }

            // Send transaction
            TransactionReceipt receipt = contract.storeHash(
                    hashBytes,
                    receiverAddress
            ).send();

            return receipt.getTransactionHash();
        } catch (Exception e) {
            throw new RuntimeException("Failed to store message hash: " + e.getMessage(), e);
        }
    }

    public boolean verifyMessage(String message) throws Exception {
        if (message == null || message.isEmpty()) {
            return false;
        }

        try {
            String hash = org.web3j.crypto.Hash.sha3(message);
            byte[] hashBytes = Numeric.hexStringToByteArray(hash);

            if (hashBytes.length > 32) {
                hashBytes = Arrays.copyOfRange(hashBytes, 0, 32);
            }

            BigInteger timestamp = contract.verifyHash(hashBytes).send();
            return timestamp.compareTo(BigInteger.ZERO) > 0;
        } catch (Exception e) {
            throw new RuntimeException("Verification failed: " + e.getMessage(), e);
        }
    }

    private boolean isValidEthAddress(String address) {
        return address != null
                && address.matches("^0x[a-fA-F0-9]{40}$");
    }
}
/*
import org.web3j.crypto.Credentials;
        import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
        import org.web3j.tx.gas.DefaultGasProvider;
        import java.math.BigInteger;

        */
/*

public class BlockchainService {

    */
/*    address= 0xf8e81D47203A594245E36C48e151709F0C19fBe8
    id= 0x64a673718AE4861b755Efd7c3F34f89d05f9Ba47
    api key = 150a0cb40cb7ecd5228ec25168556e7c552fedd92a8f1e7673cfb835eb516b4c*//*


    private static final String INFURA_URL = "https://polygon-mainnet.infura.io/v3/api_key_";
    private static final String CONTRACT_ADDRESS = "address_key_0*_from_remex";
    private static final String PRIVATE_KEY = "api_key_";

    private Web3j web3j;
    private Credentials credentials;
    private ChatVerification contract;

    public BlockchainService() {
        this.web3j = Web3j.build(new HttpService(INFURA_URL));
        this.credentials = Credentials.create(PRIVATE_KEY);
        this.contract = ChatVerification.load(
                CONTRACT_ADDRESS,
                web3j,
                credentials,
                DefaultGasProvider.GAS_PRICE,
                DefaultGasProvider.GAS_LIMIT
        );
    }

    public String storeMessageHash(String message, String receiverAddress) throws Exception {
        // Generate SHA-256 hash
        String hash = org.web3j.crypto.Hash.sha3(message);

        // Convert to bytes32
        byte[] hashBytes = new byte[32];
        System.arraycopy(
                org.web3j.utils.Numeric.hexStringToByteArray(hash),
                0,
                hashBytes,
                0,
                32
        );

        // Store on blockchain
       TransactionReceipt receipt = contract.storeHash(
                hashBytes,
                receiverAddress
        ).send();

       return receipt.getTransactionHash();
    }

    public boolean verifyMessage(String message) throws Exception {
        String hash = org.web3j.crypto.Hash.sha3(message);
        byte[] hashBytes = new byte[32];
        System.arraycopy(
                org.web3j.utils.Numeric.hexStringToByteArray(hash),
                0,
                hashBytes,
                0,
                32
        );

        BigInteger timestamp = contract.verifyHash(hashBytes).send();
        return timestamp.compareTo(BigInteger.ZERO) > 0;
    }
}*/
