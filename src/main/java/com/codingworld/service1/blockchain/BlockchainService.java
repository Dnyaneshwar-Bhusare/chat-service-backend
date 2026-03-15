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

            // Smoke-test: call verifyHash with a dummy zero hash (read-only, no gas).
            // If the on-chain bytecode is stale/broken this throws immediately,
            // and we redeploy right now rather than failing on the first real request.
            try {
                contract.verifyHash(new byte[32]).send();
                System.out.println("✅ Contract smoke-test passed.");
            } catch (Exception smokeEx) {
                System.err.println("⚠️ Smoke-test failed on loaded contract (" + contractAddress
                        + "): " + smokeEx.getMessage() + " — redeploying...");
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
     * This bypasses web3j's deployRemoteCall which requires the binary to be EVM-compatible
     * at the Java level. Ganache processes the transaction natively.
     *
     * The binary used here is the paris-translated version from ChatVerification.BINARY.
     * If that still fails, this method falls back to eth_sendTransaction with the raw
     * SHANGHAI_BINARY — at that point the user MUST change Ganache hardfork to 'shanghai'.
     */
    private String deployContractRaw() throws Exception {
        String binary = ChatVerification.BINARY;
        System.out.println("📄 Deploying binary length: " + binary.length() / 2 + " bytes");
        System.out.println("📄 Binary prefix: " + binary.substring(0, Math.min(40, binary.length())));

        BigInteger nonce = web3j.ethGetTransactionCount(
                deployerAddress, DefaultBlockParameterName.PENDING).send().getTransactionCount();

        // Build a contract-creation transaction (to = null means contract creation)
        Transaction tx = Transaction.createContractTransaction(
                deployerAddress,
                nonce,
                BigInteger.valueOf(gasPrice),
                BigInteger.valueOf(gasLimit),
                BigInteger.ZERO,          // value = 0 ETH
                "0x" + binary             // bytecode as data field
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

        // Poll for receipt (Ganache mines instantly)
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
     */
    public String storeMessageHash(String message) throws Exception {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (contract == null) {
            throw new IllegalStateException("Blockchain contract is not initialized");
        }
        byte[] hashBytes = toBytes32(Hash.sha3(message));
        TransactionReceipt receipt =contract.storeHash(hashBytes, deployerAddress).send();
        String txHash = receipt.getTransactionHash();
        System.out.println("📦 Message hash stored on blockchain. txHash: " + txHash);
        return txHash;
    }

    /**
     * Verifies a message hash on the blockchain.
     * Returns timestamp > 0 if verified, 0 if not found.
     */
    public long verifyMessageHash(String message) throws Exception {
        if (contract == null) {
            throw new IllegalStateException("Blockchain contract is not initialized");
        }
        byte[] hashBytes = toBytes32(Hash.sha3(message));
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
     * Called when a contract interaction fails (e.g. "invalid JUMP" due to EVM version mismatch).
     * Clears the stored contract address from DB and nullifies the in-memory instance so the
     * contract is redeployed fresh on the next application restart.
     *
     * Root cause of "invalid JUMP": the contract binary was compiled with --evm-version shanghai
     * (uses PUSH0 opcode) but Ganache is running on the default "merge" hardfork which does NOT
     * support PUSH0.
     * Fix options:
     *   1. In Ganache GUI → Settings → Chain → Hardfork → select "shanghai" → Restart Ganache
     *      (then also delete the contract_address row from blockchain_config table so it redeploys)
     *   2. Recompile the Solidity contract with --evm-version paris and regenerate ChatVerification.java
     */
    public void handleContractFailure() {
        //System.err.println("🔄 Contract failure detected — redeploying immediately...");
        try {
            configDao.delete(KEY_CONTRACT_ADDRESS);
            contract = null;

            // Redeploy right now so the next request doesn't get "contract is null"
            TransactionManager txManager = new ClientTransactionManager(web3j, deployerAddress);
            StaticGasProvider gasProvider = new StaticGasProvider(
                    BigInteger.valueOf(gasPrice), BigInteger.valueOf(gasLimit));

            String newAddress = deployContractRaw();
            contract = ChatVerification.load(newAddress, web3j, txManager, gasProvider);
            configDao.set(KEY_CONTRACT_ADDRESS, newAddress);
            System.out.println("✅ Contract redeployed at: " + newAddress);
        } catch (Exception ex) {
            System.err.println("❌ Redeployment in handleContractFailure failed: " + ex.getMessage());
            contract = null; // stays null; next request will get a clear error
        }
    }

    private byte[] toBytes32(String hexHash) {
        byte[] hashBytes = Numeric.hexStringToByteArray(hexHash);
        if (hashBytes.length == 32) return hashBytes;
        byte[] padded = new byte[32];
        int offset = 32 - hashBytes.length;
        System.arraycopy(hashBytes, 0, padded, Math.max(0, offset), Math.min(hashBytes.length, 32));
        return padded;
    }
}
