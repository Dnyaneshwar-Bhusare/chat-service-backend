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
    public static final String BINARY = "6080604052348015600e575f80fd5b506103968061001c5f395ff3fe608060405234801561000f575f80fd5b506004361061003f575f3560e01c80637269367b1461004357806385f09f1214610073578063ef020f4a1461008f575b5f80fd5b61005d600480360381019061005891906101f4565b6100bf565b60405161006a9190610237565b60405180910390f35b61008d600480360381019061008891906102aa565b6100d3565b005b6100a960048036038101906100a491906101f4565b6101a4565b6040516100b69190610237565b60405180910390f35b5f602052805f5260405f205f915090505481565b5f805f8481526020019081526020015f205414610125576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161011c90610342565b60405180910390fd5b425f808481526020019081526020015f20819055508073ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16837f87114e1f2736d94a9328671e9f05cf15e9777a1f425eec53fcf8180f423b1cf1426040516101989190610237565b60405180910390a45050565b5f805f8381526020019081526020015f20549050919050565b5f80fd5b5f819050919050565b6101d3816101c1565b81146101dd575f80fd5b50565b5f813590506101ee816101ca565b92915050565b5f60208284031215610209576102086101bd565b5b5f610216848285016101e0565b91505092915050565b5f819050919050565b6102318161021f565b82525050565b5f60208201905061024a5f830184610228565b92915050565b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f61027982610250565b9050919050565b6102898161026f565b8114610293575f80fd5b50565b5f813590506102a481610280565b92915050565b5f80604083850312156102c0576102bf6101bd565b5b5f6102cd858286016101e0565b92505060206102de85828601610296565b9150509250929050565b5f82825260208201905092915050565b7f4861736820616c726561647920657869737473000000000000000000000000005f82015250565b5f61032c6013836102e8565b9150610337826102f8565b602082019050919050565b5f6020820190508181035f83015261035981610320565b905091905056fea26469706673582212209bda62e9950e5aa7dc840edd47f107d5acfbdf71b5f8ef509d3717dbc43b4e9464736f6c634300081a0033";

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
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class MessageHashStoredEventResponse extends BaseEventResponse {
        public byte[] messageHash;

        public String sender;

        public String receiver;

        public BigInteger timestamp;
    }
}
