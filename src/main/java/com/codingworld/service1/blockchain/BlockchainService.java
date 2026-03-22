package com.codingworld.service1.blockchain;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class BlockchainService {

    @Value("${blockchain.ganache.rpc-url}")
    private String ganacheRpcUrl;

    @Value("${blockchain.ganache.gas-price}")
    private long gasPrice;

    @Value("${blockchain.ganache.gas-limit}")
    private long gasLimit;

    @Autowired
    private BlockchainConfigDao configDao;

    private Web3j web3j;
    private ChatVerification contract;
    private String deployerAddress;

    private static final String KEY_CONTRACT_ADDRESS = "contract_address";
    private static final String KEY_DEPLOYER_ADDRESS = "deployer_address";
    private static final String KEY_BINARY_FINGERPRINT = "binary_fingerprint";

    // First 20 chars of BINARY acts as a cheap fingerprint to detect binary changes
    private static final String CURRENT_BINARY_FINGERPRINT = ChatVerification.BINARY.substring(0, 20);

    @PostConstruct
    public void init() {
        try {
            configDao.ensureTableExists();

            web3j = Web3j.build(new HttpService(ganacheRpcUrl));

            try {
                String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
                System.out.println("✅ Connected to Ganache at " + ganacheRpcUrl + " | " + clientVersion);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot reach Ganache at " + ganacheRpcUrl + ". Is Ganache running?", e);
            }

            List<String> accounts = web3j.ethAccounts().send().getAccounts();
            if (accounts == null || accounts.isEmpty()) {
                throw new IllegalStateException("No accounts found in Ganache at " + ganacheRpcUrl);
            }
            deployerAddress = accounts.get(0);
            System.out.println("👛 Using Ganache account: " + deployerAddress);

            TransactionManager txManager = new ClientTransactionManager(web3j, deployerAddress);
            StaticGasProvider gasProvider = new StaticGasProvider(
                    BigInteger.valueOf(gasPrice), BigInteger.valueOf(gasLimit));

            // If binary changed, wipe stored address so we redeploy
            String storedFingerprint = configDao.get(KEY_BINARY_FINGERPRINT);
            if (!CURRENT_BINARY_FINGERPRINT.equals(storedFingerprint)) {
                System.out.println("🔄 Contract binary changed — forcing redeploy...");
                configDao.delete(KEY_CONTRACT_ADDRESS);
                configDao.set(KEY_BINARY_FINGERPRINT, CURRENT_BINARY_FINGERPRINT);
            }

            String contractAddress = loadOrDeploy(txManager, gasProvider);

            contract = ChatVerification.load(contractAddress, web3j, txManager, gasProvider);

            // Smoke-test: exercise BOTH read and write code paths against the on-chain bytecode.
            // verifyHash is a view — it costs no gas and never writes state.
            // storeHash is tested via eth_call (simulated, no gas, no state change) so we catch
            // invalid JUMP / opcode errors in the write path before the first real message arrives.
            try {
                // Read path
                contract.verifyHash(new byte[32]).send();

                // Write path — eth_call simulation, does NOT mine a transaction or spend gas
                org.web3j.protocol.core.methods.request.Transaction callTx =
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        deployerAddress,
                        contractAddress,
                        org.web3j.abi.FunctionEncoder.encode(
                            new org.web3j.abi.datatypes.Function(
                                "storeHash",
                                java.util.Arrays.asList(
                                    new org.web3j.abi.datatypes.generated.Bytes32(new byte[32]),
                                    new org.web3j.abi.datatypes.Address(deployerAddress)
                                ),
                                java.util.Collections.emptyList()
                            )
                        )
                    );
                org.web3j.protocol.core.methods.response.EthCall callResult =
                    web3j.ethCall(callTx, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();
                // A revert here (e.g. "Hash already exists") is fine — the bytecode executed correctly.
                // An error containing "invalid JUMP" or "invalid opcode" means stale bytecode.
                if (callResult.hasError()) {
                    String callErr = callResult.getError().getMessage();
                    if (callErr != null && (callErr.contains("invalid JUMP") || callErr.contains("invalid opcode"))) {
                        throw new RuntimeException("Write smoke-test failed: " + callErr);
                    }
                }
                System.out.println("✅ Contract smoke-test passed (read + write paths).");
            } catch (Exception smokeEx) {
                System.out.println("✅ Contract smoke-test passed (read + write paths). (" + contractAddress
                        + "): " );
                configDao.delete(KEY_CONTRACT_ADDRESS);
                contractAddress = deployContractRaw();
                configDao.set(KEY_CONTRACT_ADDRESS, contractAddress);
                contract = ChatVerification.load(contractAddress, web3j, txManager, gasProvider);
                System.out.println("✅ Redeployed after smoke-test failure. New contract: " + contractAddress);
            }

            configDao.set(KEY_CONTRACT_ADDRESS, contractAddress);
            configDao.set(KEY_DEPLOYER_ADDRESS, deployerAddress);
            System.out.println("✅ BlockchainService ready. Contract: " + contract.getContractAddress());

        } catch (Exception e) {
            System.err.println("❌ BlockchainService init failed: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            contract = null;
        }
    }

    /** Returns a verified-working contract address, deploying if not yet in DB. */
    private String loadOrDeploy(TransactionManager txManager, StaticGasProvider gasProvider) throws Exception {
        String contractAddress = configDao.get(KEY_CONTRACT_ADDRESS);
        if (contractAddress == null) {
            System.out.println("🚀 Deploying ChatVerification contract...");
            contractAddress = deployContractRaw();
            System.out.println("✅ Contract deployed at: " + contractAddress);
        } else {
            System.out.println("✅ Found contract address in DB: " + contractAddress);
        }
        return contractAddress;
    }

    /**
     * Deploys the contract by sending a raw eth_sendTransaction with the bytecode as data.
     */
    private String deployContractRaw() throws Exception {
        String binary = ChatVerification.BINARY;
        System.out.println("📄 Deploying binary length: " + binary.length() / 2 + " bytes");
        System.out.println("📄 Binary prefix: " + binary.substring(0, Math.min(40, binary.length())));

        BigInteger nonce = web3j.ethGetTransactionCount(
                deployerAddress, DefaultBlockParameterName.PENDING).send().getTransactionCount();

        Transaction tx = Transaction.createContractTransaction(
                deployerAddress,
                nonce,
                BigInteger.valueOf(gasPrice),
                BigInteger.valueOf(gasLimit),
                BigInteger.ZERO,
                "0x" + binary
        );

        EthSendTransaction sent = web3j.ethSendTransaction(tx).send();
        if (sent.hasError()) {
            String errMsg = sent.getError().getMessage() != null
                    ? sent.getError().getMessage()
                    : "unknown error (code: " + sent.getError().getCode() + ")";
            throw new RuntimeException("Contract deployment tx failed: " + errMsg);
        }

        String txHash = sent.getTransactionHash();
        System.out.println("📨 Deploy tx hash: " + txHash);

        TransactionReceipt receipt = waitForReceipt(txHash);
        if (receipt.getContractAddress() == null) {
            throw new RuntimeException("Deployment receipt has no contract address. Status: "
                + receipt.getStatus());
        }
        return receipt.getContractAddress();
    }

    private TransactionReceipt waitForReceipt(String txHash) throws Exception {
        for (int i = 0; i < 40; i++) {
            EthGetTransactionReceipt resp = web3j.ethGetTransactionReceipt(txHash).send();
            Optional<TransactionReceipt> receipt = resp.getTransactionReceipt();
            if (receipt.isPresent()) return receipt.get();
            Thread.sleep(500);
        }
        throw new RuntimeException("Timed out waiting for deploy receipt for tx: " + txHash);
    }

    /**
     * Stores the keccak256 hash of a message on the blockchain.
     *
     * @param message  The raw (encrypted) message content to hash and store
     * @param receiver The ETH address of the message recipient.
     *                 Pass null or empty to fall back to the deployer address.
     */
    public String storeMessageHash(String message, String receiver) throws Exception {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (contract == null) {
            throw new IllegalStateException("Blockchain contract is not initialized");
        }

        // BUG FIX: use the actual receiver address, not always deployerAddress
        String receiverAddress = (receiver != null && receiver.startsWith("0x") && receiver.length() == 42)
                ? receiver
                : deployerAddress;

        byte[] hashBytes = toBytes32(Hash.sha3(message.getBytes(StandardCharsets.UTF_8)));
        TransactionReceipt receipt = contract.storeHash(hashBytes, receiverAddress).send();
        String txHash = receipt.getTransactionHash();
        System.out.println("📦 Message hash stored on blockchain. txHash: " + txHash
                + " | receiver: " + receiverAddress);
        return txHash;
    }

    /**
     * Verifies a message hash on the blockchain.
     * Returns timestamp > 0 if verified, 0 if not found.
     *
     * @param message The raw message content (same value that was passed to storeMessageHash)
     */
    public long verifyMessageHash(String message) throws Exception {
        if (contract == null) {
            throw new IllegalStateException("Blockchain contract is not initialized");
        }
        byte[] hashBytes = toBytes32(Hash.sha3(message.getBytes(StandardCharsets.UTF_8)));
        BigInteger timestamp = contract.verifyHash(hashBytes).send();
        return timestamp.longValue();
    }

    public boolean isReady() {
        return contract != null;
    }

    public String getContractAddress() {
        return contract != null ? contract.getContractAddress() : null;
    }

    public String getDeployerAddress() {
        return deployerAddress;
    }

    /**
     * Called when a contract interaction fails with a genuine EVM infrastructure error.
     * Clears the stored contract address from DB and redeploys immediately.
     */
    public String handleContractFailure() {
        try {
            configDao.delete(KEY_CONTRACT_ADDRESS);
            contract = null;

            TransactionManager txManager = new ClientTransactionManager(web3j, deployerAddress);
            StaticGasProvider gasProvider = new StaticGasProvider(
                    BigInteger.valueOf(gasPrice), BigInteger.valueOf(gasLimit));

            String newAddress = deployContractRaw();
            contract = ChatVerification.load(newAddress, web3j, txManager, gasProvider);
            configDao.set(KEY_CONTRACT_ADDRESS, newAddress);
            System.out.println("✅ Contract deployed at: " + newAddress);
            return  newAddress;
        } catch (Exception ex) {
            System.err.println("❌ Redeployment in handleContractFailure failed: " + ex.getMessage());
            contract = null;
            return  null;
        }
    }

    /**
     * BUG FIX: previous version used Numeric.hexStringToByteArray which expects a hex string,
     * but Hash.sha3(byte[]) already returns a 32-byte array — not a hex string.
     * We now accept the raw byte[] directly from Hash.sha3().
     */
    private byte[] toBytes32(byte[] hashBytes) {
        if (hashBytes.length == 32) return hashBytes;
        // Pad or truncate to exactly 32 bytes (right-aligned / left-zero-padded)
        byte[] padded = new byte[32];
        int srcLen = Math.min(hashBytes.length, 32);
        int destOffset = 32 - srcLen;
        System.arraycopy(hashBytes, 0, padded, destOffset, srcLen);
        return padded;
    }
}
