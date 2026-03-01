package com.codingworld.service1.blockchain;


import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

@Service
public class BlockchainService {

    @Value("${blockchain.ganache.rpc-url}")
    private String ganacheRpcUrl;

    @Value("${blockchain.contract.address}")
    private String contractAddress;

    @Value("${blockchain.wallet.private-key}")
    private String privateKey;

    @Value("${blockchain.ganache.gas-price}")
    private long gasPrice;

    @Value("${blockchain.ganache.gas-limit}")
    private long gasLimit;

    @Value("${blockchain.ganache.network-id}")
    private long networkId;

    private Web3j web3j;
    private ChatVerification contract;

    @PostConstruct
    public void init() {
        try {
            web3j = Web3j.build(new HttpService(ganacheRpcUrl));

            Credentials credentials = Credentials.create(privateKey);

            StaticGasProvider gasProvider = new StaticGasProvider(
                    BigInteger.valueOf(gasPrice),
                    BigInteger.valueOf(gasLimit)
            );

            RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, networkId);

            contract = ChatVerification.load(contractAddress, web3j, txManager, gasProvider);

            System.out.println("✅ BlockchainService initialized. Contract loaded at: " + contractAddress);
        } catch (Exception e) {
            System.err.println("❌ BlockchainService init failed: " + e.getMessage());
        }
    }

    /**
     * Store the SHA3 hash of a message on the blockchain.
     * @param message  plaintext or encrypted message
     * @param receiverEthAddress  Ethereum address of the receiver (from Ganache accounts)
     * @return transaction hash (txHash)
     */
    public String storeMessageHash(String message, String receiverEthAddress) throws Exception {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (receiverEthAddress == null || !receiverEthAddress.startsWith("0x")) {
            throw new IllegalArgumentException("Invalid Ethereum address: " + receiverEthAddress);
        }

        // Generate keccak256 hash of message -> bytes32
        byte[] hashBytes = toBytes32(Hash.sha3(message));

        var receipt = contract.storeHash(hashBytes, receiverEthAddress).send();
        String txHash = receipt.getTransactionHash();
        System.out.println("📦 Message hash stored on blockchain. txHash: " + txHash);
        return txHash;
    }

    /**
     * Verify a message hash on blockchain — returns the timestamp it was stored, or 0 if not found.
     * @param message  the original message
     * @return timestamp (epoch seconds) when hash was stored, 0 if not found
     */
    public long verifyMessageHash(String message) throws Exception {
        byte[] hashBytes = toBytes32(Hash.sha3(message));
        BigInteger timestamp = contract.verifyHash(hashBytes).send();
        return timestamp.longValue();
    }

    public Web3j getWeb3j() {
        return web3j;
    }

    // --- Helpers ---

    private byte[] toBytes32(String hexHash) {
        byte[] hashBytes = Numeric.hexStringToByteArray(hexHash);
        if (hashBytes.length == 32) return hashBytes;
        byte[] padded = new byte[32];
        int offset = 32 - hashBytes.length;
        System.arraycopy(hashBytes, 0, padded, Math.max(0, offset), Math.min(hashBytes.length, 32));
        return padded;
    }
}
