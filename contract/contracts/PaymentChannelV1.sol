// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC20/utils/SafeERC20.sol";
import "@openzeppelin/contracts/utils/cryptography/ECDSA.sol";

// Uncomment this line to use console.log
// import "hardhat/console.sol";

contract PaymentChannelV1 is ReentrancyGuard {
    using SafeERC20 for IERC20;
    using ECDSA for bytes32;

    error MissingAllowance();
    error InvalidParticipants();
    error InvalidAmount();
    error InvalidSignature(address signer, bytes32 messageHash);
    error ChannelDoesNotExist();
    error ChannelDoesExist();

    event ChannelCreated(
        bytes32 indexed id,
        address indexed server,
        address client,
        address token,
        uint256 amount
    );

    event ChannelClosed(bytes32 indexed id);

    modifier channelMustExist(bytes32 _channelId) {
        if (!doesChannelExist(_channelId)) {
            revert ChannelDoesNotExist();
        }
        _;
    }

    modifier channelMustNotExist(bytes32 _channelId) {
        if (doesChannelExist(_channelId)) {
            revert ChannelDoesExist();
        }
        _;
    }

    struct Channel {
        address server;
        address client;
        address token;
        uint256 amount;
    }

    // The User structure defines the data to be validated
    // in the signature
    struct User {
        address user;
        uint256 expiresAt;
    }

    // The User structure defines the data to be validated
    // in the signature
    struct ChannelClose {
        bytes32 channelId;
        uint256 serverAmount;
        uint256 clientAmount;
    }

    mapping(bytes32 => Channel) channels;

    bytes32 private constant SALT = "REPLACE_ME";
    bytes32 private DOMAIN_SEPARATOR;

    // This is the type hash of the EIP712 domain structure
    bytes32 private constant EIP712_DOMAIN_TYPE_HASH =
        keccak256(
            "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract,bytes32 salt)"
        );

    // This is the type hash of the User structure
    bytes32 private constant USER_TYPE_HASH =
        keccak256("User(address user,uint256 expiresAt)");

    // This is the type hash of the channel close structure
    bytes32 private constant CHANNEL_CLOSE_TYPE_HASH =
        keccak256(
            "ChannelClose(bytes32 channelId,uint256 serverAmount,uint256 clientAmount)"
        );

    constructor() {
        DOMAIN_SEPARATOR = keccak256(
            abi.encode(
                keccak256(
                    "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract,bytes32 salt)"
                ),
                keccak256(bytes("EVMBitstream")),
                keccak256(bytes("1")),
                block.chainid,
                address(this),
                SALT
            )
        );
    }

    function createChannel(
        bytes32 _channelId,
        address _server,
        address _client,
        address _token,
        uint256 _amount
    ) external channelMustNotExist(_channelId) nonReentrant {
        if (_amount <= 0) {
            revert InvalidAmount();
        }

        if (
            _client == _server || _client == address(0) || _server == address(0)
        ) {
            revert InvalidParticipants();
        }

        IERC20 token = IERC20(_token);
        if (token.allowance(msg.sender, address(this)) < _amount) {
            revert MissingAllowance();
        }

        channels[_channelId] = Channel(_server, _client, _token, _amount);

        // This contract becomes the temporary owner of the tokens until the
        // seller fetches them.
        token.safeTransferFrom(_client, address(this), _amount);

        emit ChannelCreated(_channelId, _server, _client, _token, _amount);
    }

    function closeChannel(
        bytes32 _channelId,
        uint256 _serverAmount,
        uint256 _clientAmount,
        bytes memory _serverSignature,
        bytes memory _clientSignature
    ) external channelMustExist(_channelId) nonReentrant {
        bytes32 channelCloseHash = hashChannelClose(
            ChannelClose({
                channelId: _channelId,
                serverAmount: _serverAmount,
                clientAmount: _clientAmount
            })
        );

        Channel storage channel = channels[_channelId];

        // Verify the validity of the two signature.
        verifySignature(channelCloseHash, _clientSignature, channel.client);
        verifySignature(channelCloseHash, _serverSignature, channel.server);

        // If both signatures are valid and both parties came to an agreement perform
        // some last sanity checks before closing the channel.
        if (_serverAmount + _clientAmount != channel.amount) {
            revert InvalidAmount();
        }

        IERC20 token = IERC20(channel.token);
        token.safeTransferFrom(address(this), channel.server, _serverAmount);
        token.safeTransferFrom(address(this), channel.client, _clientAmount);

        delete channels[_channelId];

        emit ChannelClosed(_channelId);
    }

    /**
     * This triggers the force close condition, depending on who initiates the force close the contract will
     * either lock the server or the client funds in a timelock in which the other party can submit a newer
     * commited channel state. If this happens the cheating party is punished.
     *
     */
    /*
    function forceCloseChannel(
        bytes32 _channelId,
        uint256 _serverAmount,
        uint256 _clientAmount,
        bytes memory _clientSignature,
        bytes memory _serverSignature
    ) external channelMustExist(_channelId) nonReentrant {
        bytes32 channelCloseHash = hashChannelClose(
            ChannelClose({
                channelId: _channelId,
                serverAmount: _serverAmount,
                clientAmount: _clientAmount
            })
        );

        Channel storage channel = channels[_channelId];

        // Verify the validity of the two signature.
        verifySignature(channelCloseHash, _clientSignature, channel.client);
        verifySignature(channelCloseHash, _serverSignature, channel.server);

        // If both signatures are valid and both parties came to an agreement perform
        // some last sanity checks before closing the channel.
        if (_serverAmount + _clientAmount != channel.amount) {
            revert InvalidAmount();
        }

        IERC20 token = IERC20(channel.token);
        token.safeTransferFrom(address(this), channel.server, _serverAmount);
        token.safeTransferFrom(address(this), channel.client, _clientAmount);

        delete channels[_channelId];

        emit ChannelClosed(_channelId);
    }*/

    function doesChannelExist(bytes32 _channelId) internal view returns (bool) {
        return channels[_channelId].amount != 0;
    }

    /**
     * Verify the EIP 712 signature
     * @param messageHash The message hash for EIP 712 structure
     * @param signature The signature to be verified
     */
    function verifySignature(
        bytes32 messageHash,
        bytes memory signature,
        address signer
    ) private view {
        // Calculate the hash according to the given domain and message hash
        bytes32 digest = keccak256(
            abi.encodePacked("\x19\x01", DOMAIN_SEPARATOR, messageHash)
        );

        // Use ECDSA to recover the address from the signature
        // and compare it with the owner.
        if (digest.recover(signature) != signer) {
            revert InvalidSignature(signer, messageHash);
        }
    }

    function hashChannelClose(
        ChannelClose memory _channelClose
    ) private pure returns (bytes32) {
        return
            keccak256(
                abi.encode(
                    CHANNEL_CLOSE_TYPE_HASH,
                    _channelClose.channelId,
                    _channelClose.serverAmount,
                    _channelClose.clientAmount
                )
            );
    }
}
