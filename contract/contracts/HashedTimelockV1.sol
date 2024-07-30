// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC20/utils/SafeERC20.sol";

// Uncomment this line to use console.log
// import "hardhat/console.sol";

/**
 * @title Hashed Timelock Contracts (HTLCs) on Ethereum ERC20 and ERC1155 tokens.
 *
 * This contract provides a way to create and keep HTLCs for those ERC tokens. It
 * also handles ETH transfers which then should be wrapped in WETH.
 *
 * This is the first iteration and the following improvements could be done in the
 * future:
 *
 * - Use Permit2 https://blog.uniswap.org/permit2-integration-guide
 * - Improve privacy by pool money and allow the download seller to get payed via some
 *   sort of zkProof.
 */
contract HashedTimelockV1 {
    using SafeERC20 for IERC20;

    error MissingAllowance();
    error InvalidRecevier();
    error InvalidAmount();
    error InvalidPreimage();
    error ContractAlreadyExists();
    error ContractDoesNotExist();
    error ContractExpired();
    error ContractNotExpired();
    error NotContractOwner();
    error NotReceiver();

    event HtlcCreated(bytes32 indexed hashlock, bytes32 contractId);
    event HtlcWithdrawn(bytes32 indexed contractId, bytes32 preimage);
    event HtlcRefunded(bytes32 indexed contractId);

    struct Htlc {
        address sender;
        address receiver;
        uint256 timelock;
        bytes32 hashlock;
        address token;
        uint256 amount;
    }

    modifier withdrawable(bytes32 _contractId) {
        if (contracts[_contractId].receiver != msg.sender) {
            revert NotReceiver();
        }
        if (contracts[_contractId].receiver == address(0)) {
            revert ContractDoesNotExist();
        }
        if (contracts[_contractId].timelock > block.timestamp) {
            revert ContractExpired();
        }
        _;
    }

    mapping(bytes32 => Htlc) contracts;

    constructor() {}

    /**
     * @dev Sender / Payer sets up a new hash time lock contract depositing the
     * funds and providing the reciever and terms.
     *
     * NOTE: _receiver must first call approve() on the token contract.
     *       See allowance check in tokensTransferable modifier.
     *
     */
    function newContract(
        bytes32 _hashlock,
        uint256 _timelock,
        address _receiver,
        address _token,
        uint256 _amount
    ) external returns (bytes32 contractId) {
        contractId = getContractId(_hashlock);

        if (_amount == 0) {
            revert InvalidAmount();
        }

        if (IERC20(_token).allowance(msg.sender, address(this)) < _amount) {
            revert MissingAllowance();
        }

        if (_timelock <= block.timestamp) {
            revert ContractExpired();
        }

        if (_receiver == address(0)) {
            revert InvalidRecevier();
        }

        // Reject if a contract already exists with the same parameters. The
        // sender must change one of these parameters (ideally providing a
        // different _hashlock).
        if (doesContractExist(contractId)) {
            revert ContractAlreadyExists();
        }

        contracts[contractId] = Htlc(
            msg.sender,
            _receiver,
            _timelock,
            _hashlock,
            _token,
            _amount
        );

        // This contract becomes the temporary owner of the tokens until the
        // seller fetches them.
        IERC20(_token).safeTransferFrom(msg.sender, address(this), _amount);

        emit HtlcCreated(_hashlock, contractId);
    }

    function refund(bytes32 _contractId) external {
        if (!doesContractExist(_contractId)) {
            revert ContractDoesNotExist();
        }

        Htlc storage htlc = contracts[_contractId];

        if (htlc.timelock >= block.timestamp) {
            revert ContractNotExpired();
        }

        if (htlc.sender != msg.sender) {
            revert NotContractOwner();
        }

        // Safe variables so they are not erased.
        address token = htlc.token;
        uint256 amount = htlc.amount;

        delete contracts[_contractId];

        IERC20(token).safeTransfer(msg.sender, amount);

        emit HtlcRefunded(_contractId);
    }

    /**
     * @dev Called by the receiver once the sender deposited the funds in the Hashlock.
     * To transfer the tokens into their address, the preimage must be revealed.
     *
     * @param _preimage sha256(_preimage) should equal the contract hashlock.
     */
    function withdraw(bytes32 _contractId, bytes32 _preimage) external {
        if (!doesContractExist(_contractId)) {
            revert ContractDoesNotExist();
        }

        Htlc storage htlc = contracts[_contractId];

        if (htlc.timelock < block.timestamp) {
            revert ContractExpired();
        }

        if (htlc.receiver != msg.sender) {
            revert NotReceiver();
        }

        if (sha256(abi.encodePacked(_preimage)) != htlc.hashlock) {
            revert InvalidPreimage();
        }

        // Safe variables so they are not erased.
        address token = htlc.token;
        uint256 amount = htlc.amount;

        delete contracts[_contractId];

        IERC20(token).safeTransfer(msg.sender, amount);

        emit HtlcWithdrawn(_contractId, _preimage);
    }

    function doesContractExist(
        bytes32 _contractId
    ) internal view returns (bool) {
        return contracts[_contractId].receiver != address(0);
    }

    function getContractId(
        bytes32 _hashlock
    ) internal pure returns (bytes32 contractId) {
        return sha256(abi.encodePacked("evmbitstream", _hashlock));
    }
}
