package com.codingworld.service1.blockchain;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;  // GANACHE DISABLED
import org.springframework.stereotype.Service;
// import org.web3j.crypto.Hash;                              // GANACHE DISABLED
// import org.web3j.protocol.Web3j;                           // GANACHE DISABLED
// import org.web3j.protocol.core.DefaultBlockParameterName;  // GANACHE DISABLED
// import org.web3j.protocol.core.methods.request.Transaction; // GANACHE DISABLED
// import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt; // GANACHE DISABLED
// import org.web3j.protocol.core.methods.response.EthSendTransaction;       // GANACHE DISABLED
// import org.web3j.protocol.core.methods.response.TransactionReceipt;       // GANACHE DISABLED
// import org.web3j.protocol.http.HttpService;                // GANACHE DISABLED
// import org.web3j.tx.ClientTransactionManager;              // GANACHE DISABLED
// import org.web3j.tx.TransactionManager;                    // GANACHE DISABLED
// import org.web3j.tx.gas.StaticGasProvider;                 // GANACHE DISABLED
// import java.math.BigInteger;                               // GANACHE DISABLED
// import java.nio.charset.StandardCharsets;                  // GANACHE DISABLED
// import java.util.List;                                     // GANACHE DISABLED
// import java.util.Optional;                                 // GANACHE DISABLED

@Service
public class BlockchainService {

    // =========================================================
    // GANACHE DISABLED FOR TESTING — uncomment everything when Ganache is up
    // =========================================================

    // @Value("${blockchain.ganache.rpc-url}")
    // private String ganacheRpcUrl;

    // @Value("${blockchain.ganache.gas-price}")
    // private long gasPrice;

    // @Value("${blockchain.ganache.gas-limit}")
    // private long gasLimit;

    @Autowired
    private BlockchainConfigDao configDao;

    // private Web3j web3j;
    // private ChatVerification contract;
    // private String deployerAddress;

    // private static final String KEY_CONTRACT_ADDRESS     = "contract_address";
    // private static final String KEY_DEPLOYER_ADDRESS     = "deployer_address";
    // private static final String KEY_BINARY_FINGERPRINT   = "binary_fingerprint";
    // private static final String CURRENT_BINARY_FINGERPRINT = ChatVerification.BINARY.substring(0, 20);

    @PostConstruct
    public void init() {
        System.out.println("⚠️  BlockchainService: Ganache is DISABLED for testing. All blockchain calls are no-ops.");

        // ---- GANACHE INIT COMMENTED OUT FOR TESTING ----
        /*
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
                if (callResult.hasError()) {
                    String callErr = callResult.getError().getMessage();
                    if (callErr != null && (callErr.contains("invalid JUMP") || callErr.contains("invalid opcode"))) {
                        throw new RuntimeException("Write smoke-test failed: " + callErr);
                    }
                }
                System.out.println("✅ Contract smoke-test passed (read + write paths).");
            } catch (Exception smokeEx) {
                System.out.println("✅ Contract smoke-test passed (read + write paths). (" + contractAddress + "): ");
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
            contract = null;
        }
        */
    }

    // ---- GANACHE METHODS COMMENTED OUT FOR TESTING ----

    /*
    @SuppressWarnings("unused")
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

    private byte[] toBytes32(byte[] hashBytes) {
        if (hashBytes.length == 32) return hashBytes;
        byte[] padded = new byte[32];
        int srcLen = Math.min(hashBytes.length, 32);
        int destOffset = 32 - srcLen;
        System.arraycopy(hashBytes, 0, padded, destOffset, srcLen);
        return padded;
    }
    */

    // =========================================================
    // NO-OP STUBS — active while Ganache is disabled
    // =========================================================

    /**
     * NO-OP stub while Ganache is disabled.
     * Original: hashes the message and stores it on-chain via ChatVerification.storeHash().
     */
    public String storeMessageHash(String message, String receiver) throws Exception {
        System.out.println("⚠️  BlockchainService.storeMessageHash() called but Ganache is disabled — skipping.");
        return "ganache-disabled";

        /*  --- ORIGINAL IMPLEMENTATION (restore when Ganache is enabled) ---
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (contract == null) {
            throw new IllegalStateException("Blockchain contract is not initialized");
        }
        String receiverAddress = (receiver != null && receiver.startsWith("0x") && receiver.length() == 42)
                ? receiver : deployerAddress;
        byte[] hashBytes = toBytes32(Hash.sha3(message.getBytes(StandardCharsets.UTF_8)));
        TransactionReceipt receipt = contract.storeHash(hashBytes, receiverAddress).send();
        String txHash = receipt.getTransactionHash();
        System.out.println("📦 Message hash stored on blockchain. txHash: " + txHash + " | receiver: " + receiverAddress);
        return txHash;
        */
    }

    /**
     * NO-OP stub while Ganache is disabled.
     * Original: verifies a message hash on-chain via ChatVerification.verifyHash().
     */
    public long verifyMessageHash(String message) throws Exception {
        System.out.println("⚠️  BlockchainService.verifyMessageHash() called but Ganache is disabled — returning 0.");
        return 0L;

        /*  --- ORIGINAL IMPLEMENTATION (restore when Ganache is enabled) ---
        if (contract == null) {
            throw new IllegalStateException("Blockchain contract is not initialized");
        }
        byte[] hashBytes = toBytes32(Hash.sha3(message.getBytes(StandardCharsets.UTF_8)));
        BigInteger timestamp = contract.verifyHash(hashBytes).send();
        return timestamp.longValue();
        */
    }

    /** Returns false while Ganache is disabled. Original: return contract != null; */
    public boolean isReady() {
        return false;
    }

    /** Returns null while Ganache is disabled. Original: return contract.getContractAddress(); */
    public String getContractAddress() {
        return null;
    }

    /** Returns null while Ganache is disabled. Original: return deployerAddress; */
    public String getDeployerAddress() {
        return null;
    }

    /** NO-OP stub while Ganache is disabled. */
    public String handleContractFailure() {
        System.out.println("⚠️  BlockchainService.handleContractFailure() called but Ganache is disabled — skipping.");
        return null;
    }
}
