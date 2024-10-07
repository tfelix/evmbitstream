// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC20/utils/SafeERC20.sol";
import "@openzeppelin/contracts/utils/cryptography/ECDSA.sol";
import "@openzeppelin/contracts/utils/cryptography/EIP712.sol";

// Uncomment this line to use console.log
import "hardhat/console.sol";

contract PaymentChannelV1 is ReentrancyGuard, EIP712 {
    using SafeERC20 for IERC20;
    using ECDSA for bytes32;

    error MissingAllowance();
    error InvalidParticipants();
    error InvalidAmount();
    error InvalidSignature(address signer, bytes32 channelId);
    error ChannelDoesNotExist();
    error ChannelDoesExist();
    error InvalidChannelState();
    error ChannelTimelockNotExpired();

    event ChannelCreated(
        bytes32 indexed id,
        address indexed server,
        address client,
        address token,
        uint256 amount
    );

    event ServerBondVested(
        address indexed server,
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
        uint256 closingNonce;
        uint256 closingBlock;
    }

    struct ServerBond {
        uint256 amount;
        uint256 bondUnlockBlock;
    }

    // The User structure defines the data to be validated
    // in the signature
    struct ChannelClose {
        bytes32 channelId;
        uint256 serverAmount;
        uint256 clientAmount;
        uint256 inflightAmount;
        uint256 sequence;
    }

    struct ChannelUpdate {
        uint256 inflightAmount;
        uint256 lockedUntil;
    }

    mapping(bytes32 => Channel) channels;
    mapping(address => mapping(address => ServerBond)) serverBonds;

    // This is the type hash of the User structure
    bytes32 private constant USER_TYPE_HASH =
        keccak256("User(address user,uint256 expiresAt)");

    // This is the type hash of the channel close structure
    bytes32 private constant CHANNEL_CLOSE_TYPE_HASH =
        keccak256(
            "ChannelClose(bytes32 channelId,uint256 serverAmount,uint256 clientAmount,uint256 inflightAmount,uint256 sequence)"
        );

    // 48h
    uint256 private constant FORCE_CLOSE_BLOCK_DELAY = 14_400;

    // 1h
    uint256 private constant SERVER_BOND_UNLOCK_DELAY = 300;

    constructor() EIP712("EVMBitstream", "1") {}

    function addServerBond(
        address _token,
        uint256 _amount
    ) public nonReentrant {
        ServerBond storage existingServerBond = serverBonds[msg.sender][_token];

        if (existingServerBond.amount != 0) {
            // TODO
        }

        IERC20 token = IERC20(_token);
        token.safeTransferFrom(msg.sender, address(this), _amount);

        serverBonds[msg.sender][_token] = ServerBond({
            amount: _amount,
            bondUnlockBlock: 0
        });
    }

    function getServerBond(
        address _server,
        address _token
    ) public view returns (uint256) {
        ServerBond storage existingServerBond = serverBonds[_server][_token];

        if (existingServerBond.bondUnlockBlock != 0) {
            return 0;
        } else {
            return existingServerBond.amount;
        }
    }

    function undoServerBond(address _token) external nonReentrant {
        ServerBond storage existingServerBond = serverBonds[msg.sender][_token];

        if (
            existingServerBond.bondUnlockBlock != 0 ||
            existingServerBond.amount == 0
        ) {
            revert();
        } else {
            existingServerBond.bondUnlockBlock = block.number;
        }
    }

    function claimServerBond(address _token) external nonReentrant {
        ServerBond storage existingServerBond = serverBonds[msg.sender][_token];

        if (
            existingServerBond.bondUnlockBlock + SERVER_BOND_UNLOCK_DELAY <
            block.number ||
            existingServerBond.amount == 0
        ) {
            revert();
        } else {
            IERC20 token = IERC20(_token);
            token.safeTransfer(address(this), existingServerBond.amount);
            delete serverBonds[msg.sender][_token];
        }
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

        channels[_channelId] = Channel(_server, _client, _token, _amount, 0, 0);

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
        ChannelClose memory channelClose = ChannelClose({
            channelId: _channelId,
            serverAmount: _serverAmount,
            clientAmount: _clientAmount,
            inflightAmount: 0x0,
            // inflightSecret: bytes32(0),
            sequence: 0x0
        });

        Channel storage channel = channels[_channelId];

        // Verify the validity of the two signature.
        _verifyChannelClose(channelClose, _clientSignature, channel.client);
        _verifyChannelClose(channelClose, _serverSignature, channel.server);

        // If both signatures are valid and both parties came to an agreement perform
        // some last sanity checks before closing the channel.
        if (_serverAmount + _clientAmount != channel.amount) {
            revert InvalidAmount();
        }

        IERC20 token = IERC20(channel.token);
        token.safeTransfer(channel.server, _serverAmount);
        token.safeTransfer(channel.client, _clientAmount);

        delete channels[_channelId];

        emit ChannelClosed(_channelId);
    }

    /**
     * This triggers the force close condition, depending on who initiates the force close the contract will
     * either lock the server or the client funds in a timelock in which the other party can submit a newer
     * commited channel state. If this happens the cheating party is punished.
     *
     */
    function forceCloseChannel(
        ChannelClose memory _channelClose,
        bytes memory _clientSignature,
        bytes memory _serverSignature
    ) external channelMustExist(_channelClose.channelId) nonReentrant {
        Channel storage channel = channels[_channelClose.channelId];

        console.log("serverAmount: %s", _channelClose.serverAmount);
        console.log("clientAmount: %s", _channelClose.clientAmount);
        console.log("flight: %s", _channelClose.inflightAmount);
        console.log("seq: %s", _channelClose.sequence);

        // Verify the validity of the two signature.
        _verifyChannelClose(_channelClose, _clientSignature, channel.client);
        _verifyChannelClose(_channelClose, _serverSignature, channel.server);

        // If both signatures are valid and both parties came to an agreement perform
        // some last sanity checks before closing the channel.
        uint256 totalChannelAmount = _channelClose.serverAmount +
            _channelClose.clientAmount +
            _channelClose.inflightAmount;
        if (totalChannelAmount != channel.amount) {
            revert InvalidAmount();
        }

        if (channel.closingBlock != 0) {
            revert InvalidChannelState();
        }

        // We now start the timelock process in which the other party can submit a newer channel
        // state so the cheating party is punished.
        channel.closingBlock = block.number;
        channel.closingNonce = _channelClose.sequence;
    }

    function claimForceClosedChannel(
        ChannelClose memory _channelClose,
        bytes memory _clientSignature,
        bytes memory _serverSignature
    ) external channelMustExist(_channelClose.channelId) nonReentrant {
        Channel storage channel = channels[_channelClose.channelId];

        // Verify the validity of the two signatures.
        _verifyChannelClose(_channelClose, _clientSignature, channel.client);
        _verifyChannelClose(_channelClose, _serverSignature, channel.server);

        // If both signatures are valid and both parties came to an agreement perform
        // some last sanity checks before closing the channel.
        uint256 totalChannelAmount = _channelClose.serverAmount +
            _channelClose.clientAmount +
            _channelClose.inflightAmount;
        if (totalChannelAmount != channel.amount) {
            revert InvalidAmount();
        }

        if (channel.closingBlock == 0) {
            revert InvalidChannelState();
        }

        if (channel.closingBlock + FORCE_CLOSE_BLOCK_DELAY > block.number) {
            revert ChannelTimelockNotExpired();
        }

        IERC20 token = IERC20(channel.token);
        token.safeTransfer(
            channel.server,
            _channelClose.serverAmount + _channelClose.inflightAmount
        );
        token.safeTransfer(channel.client, _channelClose.clientAmount);

        delete channels[_channelClose.channelId];

        emit ChannelClosed(_channelClose.channelId);
    }

    function doesChannelExist(bytes32 _channelId) internal view returns (bool) {
        return channels[_channelId].amount != 0;
    }

    /// @notice Verifies the signature for a given Ticket, returning the address of the signer.
    /// @dev Will revert if the signature is invalid.
    /// @param channelClose A ticket describing an event
    /// @param signature The ticket seller signature
    function _verifyChannelClose(
        ChannelClose memory channelClose,
        bytes memory signature,
        address signer
    ) internal view {
        bytes32 digest = hashChannelClose(channelClose);
        address generatedSigner = ECDSA.recover(digest, signature);

        if (signer != generatedSigner) {
            revert InvalidSignature(generatedSigner, channelClose.channelId);
        }
    }

    /// @notice Returns a hash of a given Ticket, prepared using EIP712 typed data hashing rules.
    /// @param _channelClose A ticket describing an event
    function hashChannelClose(
        ChannelClose memory _channelClose
    ) internal view returns (bytes32) {
        return
            _hashTypedDataV4(
                keccak256(
                    abi.encode(
                        CHANNEL_CLOSE_TYPE_HASH,
                        _channelClose.channelId,
                        _channelClose.serverAmount,
                        _channelClose.clientAmount,
                        _channelClose.inflightAmount,
                        // _channelClose.inflightSecret,
                        _channelClose.sequence
                    )
                )
            );
    }
}
