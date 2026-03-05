// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

contract ChatVerification {
    mapping(bytes32 => uint256) public messageTimestamps;

    event MessageHashStored(
        bytes32 indexed messageHash,
        address indexed sender,
        address indexed receiver,
        uint256 timestamp
    );

    function storeHash(bytes32 hash, address receiver) external {
        require(messageTimestamps[hash] == 0, "Hash already exists");
        messageTimestamps[hash] = block.timestamp;
        emit MessageHashStored(hash, msg.sender, receiver, block.timestamp);
    }

    function verifyHash(bytes32 hash) external view returns (uint256) {
        return messageTimestamps[hash];
    }
}

