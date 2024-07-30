import {
  loadFixture,
} from "@nomicfoundation/hardhat-toolbox-viem/network-helpers";
import { assert, expect } from "chai";
import hre from "hardhat";

describe("HashedTimelock2", function () {
  // We define a fixture to reuse the same setup in every test.
  // We use loadFixture to run this setup once, snapshot that state,
  // and reset Hardhat Network to that snapshot in every test.
  async function deployFixture() {
    // Contracts are deployed using the first signer/account by default
    const [buyer, seller, other] = await hre.viem.getWalletClients();

    const sut = await hre.viem.deployContract("HashedTimelockV1");
    const erc20 = await hre.viem.deployContract("MockERC20", [10_000n]);

    const publicClient = await hre.viem.getPublicClient();

    return {
      sut,
      erc20,
      buyer,
      seller,
      other,
      publicClient,
    };
  }

  describe("newContract", function () {
    describe("Validations", function () {
      it("Should revert if ERC20 amount is 0", async function () {
        const { sut, seller, erc20 } = await loadFixture(deployFixture);

        const client = await hre.viem.getPublicClient();
        const timelock = (await client.getBlock()).number + 10n;

        await erc20.write.approve([sut.address, 10n]);

        await expect(sut.write.newContract([
          '0x0000000000000000000000000000000000000000000000000000000000000000',
          timelock,
          seller.account.address,
          erc20.address,
          0n
        ])).to.be.rejectedWith("InvalidAmount");
      });

      it("Should revert if ERC20 has not enough allowance", async function () {
        const { sut, seller, erc20 } = await loadFixture(deployFixture);

        const client = await hre.viem.getPublicClient();
        const timelock = (await client.getBlock()).number + 10n;

        await erc20.write.approve([sut.address, 0n]);

        await expect(sut.write.newContract([
          '0x0000000000000000000000000000000000000000000000000000000000000000',
          timelock,
          seller.account.address,
          erc20.address,
          10n
        ])).to.be.rejectedWith("MissingAllowance");
      });

      it("Should revert if timelock is in the past", async function () {
        const { sut, seller, erc20 } = await loadFixture(deployFixture);

        const client = await hre.viem.getPublicClient();
        const timelock = (await client.getBlock()).number + 10n;

        await erc20.write.approve([sut.address, 10n]);

        // Advance blocks past time limit
        const testClient = await hre.viem.getTestClient();
        await testClient.mine({
          blocks: 11,
        });

        await expect(sut.write.newContract([
          '0x0000000000000000000000000000000000000000000000000000000000000000',
          timelock,
          seller.account.address,
          erc20.address,
          10n
        ])).to.be.rejectedWith("ContractExpired");
      });

      it("Should revert if identical contract already exists", async function () {

      });

      it("Should revert if receiver is 0 address", async function () {

      });

      /*
      xit("Should create a good HTLC and return its ID", async function () {
        const { sut, seller, erc20 } = await loadFixture(deployFixture);

        const client = await hre.viem.getPublicClient();
        const timelock = (await client.getBlock()).number + 10n;

        await erc20.write.approve([sut.address, 10n]);

        await expect(sut.write.newContract([
          '0x0000000000000000000000000000000000000000000000000000000000000000',
          timelock,
          seller.account.address,
          erc20.address,
          10n
        ])).to.emit(sut, "HtlcCreated")
          .withArgs(
            '0x0000000000000000000000000000000000000000000000000000000000000000',
            '0x0000000000000000000000000000000000000000000000000000000000000000'
          );
      });*/
    });

    describe("Events", function () {
      it("Should emit an event on successful creation", async function () {

      });
    });
  });

  describe("refund", function () {
    describe("Validations", function () {
      it("Should revert if timelock has not yet expired", async function () {

      });

      it("Should revert if amount is a mismatch", async function () {

      });

      it("Should revert if payment was already collected", async function () {

      });

      it("Should revert if caller is not the receiver of refund", async function () {

      });

      it("Should revert if token to refund is not the one that funded it", async function () {

      });

      it("Should send funds to msg.sender if successfully refunded", async function () {

      });

      describe("Events", function () {
        it("Should emit an event on refund", async function () {

        });
      });
    });
  });

  describe("withdraw", function () {
    describe("Validations", function () {
      it("Reverts if timelock is expired", async function () {

      });

      it("Reverts if receiver mismatches", async function () {

      });

      it("Reverts if payment was already collected once", async function () {

      });

      it("Sends the funds to the receiver", async function () {

      });

      it("Reverts if preimage mismatches hashlock", async function () {

      });

      it("Reverts if amount mismatches", async function () {

      });

      describe("Events", function () {
        it("Emits an event with preimage inside", async function () {

        });
      });
    });
  });
});
