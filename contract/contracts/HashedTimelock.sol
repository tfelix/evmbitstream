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
 * Later consider using Permit2 for UX
 * https://blog.uniswap.org/permit2-integration-guide
 */
contract HashedTimelock {
    using SafeERC20 for IERC20;

    error MissingAllowance();
    error InvalidAmount();
    error InvalidHashlock();
    error InvalidPreimage();
    error ContractAlreadyExists();
    error ContractDoesNotExist();

    event HTLCNew(
        address indexed sender,
        bytes32 indexed hashlock,
        address tokenContract,
        uint256 amount,
        uint256 timelock
    );

    // Receiver, Sender, TokenID, PreImage, Hashlock, Timelock, Amount

    struct LockContractData {
        address sender;
        address receiver;
        address tokenContract;
        uint256 amount;
        bytes32 hashlock;
        uint256 timelock;
    }

    modifier tokensTransferable(
        address _token,
        address _sender,
        uint256 _amount
    ) {
        if (_amount > 0) {
            revert InvalidAmount();
        }
        if (IERC20(_token).allowance(_sender, address(this)) >= _amount) {
            revert MissingAllowance();
        }
        _;
    }

    modifier futureTimelock(uint256 _time) {
        // only requirement is the timelock time is after the last blocktime (now).
        // probably want something a bit further in the future then this.
        // but this is still a useful sanity check:
        require(_time > block.timestamp, "timelock time must be in the future");
        _;
    }

    modifier contractExists(bytes32 _contractId) {
        if (!hasContract(_contractId)) {
            revert ContractDoesNotExist();
        }
        _;
    }

    /*
    modifier withdrawable(bytes32 _contractId) {
        require(
            contracts[_contractId].receiver == msg.sender,
            "withdrawable: not receiver"
        );
        require(
            contracts[_contractId].withdrawn == false,
            "withdrawable: already withdrawn"
        );
        // This check needs to be added if claims are allowed after timeout. That is, if the following timelock check is commented out
        require(
            contracts[_contractId].refunded == false,
            "withdrawable: already refunded"
        );
        // if we want to disallow claim to be made after the timeout, uncomment the following line
        // require(contracts[_contractId].timelock > now, "withdrawable: timelock time must be in the future");
        _;
    }*/

    /*
    modifier refundable(bytes32 _contractId) {
        require(
            contracts[_contractId].sender == msg.sender,
            "refundable: not sender"
        );
        require(
            contracts[_contractId].refunded == false,
            "refundable: already refunded"
        );
        require(
            contracts[_contractId].withdrawn == false,
            "refundable: already withdrawn"
        );
        require(
            contracts[_contractId].timelock <= now,
            "refundable: timelock not yet passed"
        );
        _;
    }
    */

    mapping(bytes32 => bool) contracts;

    constructor() {}

    /**
     * @dev Sender / Payer sets up a new hash time lock contract depositing the
     * funds and providing the reciever and terms.
     *
     * NOTE: _receiver must first call approve() on the token contract.
     *       See allowance check in tokensTransferable modifier.

     * @param _hashlock A sha-2 sha256 hash hashlock.
     * @param _timelock UNIX epoch seconds time that the lock expires at.
     *                  Refunds can be made after this time.
     * @param _tokenContract ERC20 Token contract address.
     * @param _amount Amount of the token to lock up.
     * @return contractId Id of the new HTLC. This is needed for subsequent
     *                    calls.
     */
    function newContract(
        bytes32 _hashlock,
        uint256 _timelock,
        uint256 _amount,
        address _tokenContract
    )
        external
        tokensTransferable(_tokenContract, msg.sender, _amount)
        futureTimelock(_timelock)
        returns (bytes32 contractId)
    {
        contractId = getContractId(
            msg.sender,
            _hashlock,
            _timelock,
            _amount,
            _tokenContract
        );

        // Reject if a contract already exists with the same parameters. The
        // sender must change one of these parameters (ideally providing a
        // different _hashlock).
        if (hasContract(contractId)) {
            revert ContractAlreadyExists();
        }

        contracts[contractId] = true;

        // This contract becomes the temporary owner of the tokens
        IERC20(_tokenContract).safeTransferFrom(
            msg.sender,
            address(this),
            _amount
        );

        emit HTLCNew(msg.sender, _hashlock, _tokenContract, _amount, _timelock);
    }

    /**
     * @dev Called by the receiver once the sender deposited the funds in the Hashlock.
     * To transfer the tokens into their address, the preimage must be revealed.
     *
     * @param _preimage sha256(_preimage) should equal the contract hashlock.
     */
    function withdraw(
        bytes32 _preimage,
        bytes32 _hashlock,
        uint256 _timelock,
        uint256 _amount,
        address _tokenContract,
        address _receiver,
        address _sender
    ) external {
        bytes32 contractId = getContractId(
            _sender,
            _hashlock,
            _timelock,
            _amount,
            _tokenContract
        );
        if (!contracts[contractId]) {
            revert ContractDoesNotExist();
        }

        if (sha256(abi.encodePacked(_preimage)) != _hashlock) {
            revert InvalidPreimage();
        }

        contracts[contractId] = false;

        IERC20(_tokenContract).safeTransfer(_receiver, _amount);

        // emit HTLCERC20Withdraw(_contractId);
    }

    /**
     * @dev Is there a contract with id _contractId.
     * @param _contractId Id into contracts mapping.
     */
    function hasContract(bytes32 _contractId) internal view returns (bool) {
        return contracts[_contractId];
    }

    function getContractId(
        address _sender,
        bytes32 _hashlock,
        uint256 _timelock,
        uint256 _amount,
        address _tokenContract
    ) internal pure returns (bytes32 contractId) {
        return
            sha256(
                abi.encodePacked(
                    _sender,
                    _tokenContract,
                    _amount,
                    _hashlock,
                    _timelock
                )
            );
    }
}
