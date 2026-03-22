package com.codingworld.service1.blockchain;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/hyperledger-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.6.3.
 */
@SuppressWarnings("rawtypes")
public class ChatVerification extends Contract {
    // Compiled with solc 0.8.26 --evm-version paris --optimize
    // MUST match the hardfork used in start-ganache.bat (--hardfork paris)
    // If you change the .sol file or the evm-version, recompile and paste new binary here.
    public static final String BINARY = "608060405234801561001057600080fd5b50610416806100206000396000f3fe608060405234801561001057600080fd5b50600436106100415760003560e01c80637269367b1461004657806385f09f1214610078578063ef020f4a14610094575b600080fd5b610062600480360381019061005d91906101f8565b6100c4565b60405161006f9190610241565b60405180910390f35b610092600480360381019061008d91906102ae565b6100d9565b005b6100ae60048036038101906100a991906101f8565b6101aa565b6040516100bb9190610241565b60405180910390f35b60006020528060005260406000206000915090505481565b600080600084815260200190815260200160002054146101395760405162461bcd60e51b815260206004820152601360248201527f4861736820616c72656164792065786973747300000000000000000000000000604482015260640160405180910390fd5b42600080848152602001908152602001600020819055508073ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16837f87114e1f2736d94a9328671e9f05cf15e9777a1f425eec53fcf8180f423b1cf14260405161019e9190610241565b60405180910390a45050565b600080600083815260200190815260200160002054905091905056fe";

    private static String librariesLinkedBinary;

    public static final String FUNC_STOREHASH = "storeHash";
    public static final String FUNC_MESSAGETIMESTAMPS = "messageTimestamps";
    public static final String FUNC_VERIFYHASH = "verifyHash";

    public static final Event MESSAGEHASHSTORED_EVENT = new Event("MessageHashStored", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected ChatVerification(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ChatVerification(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ChatVerification(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ChatVerification(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<MessageHashStoredEventResponse> getMessageHashStoredEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(MESSAGEHASHSTORED_EVENT, transactionReceipt);
        ArrayList<MessageHashStoredEventResponse> responses = new ArrayList<MessageHashStoredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            MessageHashStoredEventResponse typedResponse = new MessageHashStoredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.messageHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.receiver = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static MessageHashStoredEventResponse getMessageHashStoredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(MESSAGEHASHSTORED_EVENT, log);
        MessageHashStoredEventResponse typedResponse = new MessageHashStoredEventResponse();
        typedResponse.log = log;
        typedResponse.messageHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.sender = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.receiver = (String) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<MessageHashStoredEventResponse> messageHashStoredEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getMessageHashStoredEventFromLog(log));
    }

    public Flowable<MessageHashStoredEventResponse> messageHashStoredEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(MESSAGEHASHSTORED_EVENT));
        return messageHashStoredEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> storeHash(byte[] hash, String receiver) {
        final Function function = new Function(
                FUNC_STOREHASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(hash), 
                new org.web3j.abi.datatypes.Address(160, receiver)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> messageTimestamps(byte[] param0) {
        final Function function = new Function(FUNC_MESSAGETIMESTAMPS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> verifyHash(byte[] hash) {
        final Function function = new Function(FUNC_VERIFYHASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(hash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    @Deprecated
    public static ChatVerification load(String contractAddress, Web3j web3j,
            Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ChatVerification(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ChatVerification load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ChatVerification(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ChatVerification load(String contractAddress, Web3j web3j,
            Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ChatVerification(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ChatVerification load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ChatVerification(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ChatVerification> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ChatVerification.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<ChatVerification> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ChatVerification.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    public static RemoteCall<ChatVerification> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ChatVerification.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<ChatVerification> deploy(Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ChatVerification.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

/*    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }*/

    private static String getDeploymentBinary() {
        return librariesLinkedBinary != null ? librariesLinkedBinary : BINARY;
    }

    private static byte[] hexToBytes(String hex) {
        byte[] d = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2)
            d[i/2] = (byte)((Character.digit(hex.charAt(i), 16) << 4)
                           + Character.digit(hex.charAt(i+1), 16));
        return d;
    }

    private static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x & 0xFF));
        return sb.toString();
    }

    public static class MessageHashStoredEventResponse extends BaseEventResponse {
        public byte[] messageHash;

        public String sender;

        public String receiver;

        public BigInteger timestamp;
    }
}
