import {
  loadFixture,
} from "@nomicfoundation/hardhat-toolbox-viem/network-helpers";
import { expect } from "chai";
import hre from "hardhat";
import { zeroAddress, WalletClient, Address, Account, keccak256, toHex, toBytes, pad } from "viem";

const channelId1 = "0xade0e4dd0af1150aace568c765e5a21f34d7e8fd1b7615cc83146ada7e9e0b75";

describe("PaymentChannelV1", function () {
  // We define a fixture to reuse the same setup in every test.
  // We use loadFixture to run this setup once, snapshot that state,
  // and reset Hardhat Network to that snapshot in every test.
  async function deployFixture() {
    // Contracts are deployed using the first signer/account by default
    const [client, server, other] = await hre.viem.getWalletClients();


    const sut = await hre.viem.deployContract("PaymentChannelV1");
    const erc20 = await hre.viem.deployContract("MockERC20");
    await erc20.write.mint([10_000n], { account: client.account });

    const publicClient = await hre.viem.getPublicClient();

    return {
      sut,
      erc20,
      client,
      server,
      other,
      publicClient,
    };
  }

  async function createChannel(
    sut: any,
    erc20: any,
    serverAccount: Account,
    clientAccount: Account,
    balance: bigint = 100n
  ) {
    await erc20.write.approve([sut.address, balance], { account: clientAccount });

    await sut.write.createChannel([
      channelId1,
      serverAccount.address,
      clientAccount.address,
      erc20.address,
      balance
    ], { account: clientAccount });
  }

  async function createChannelCloseSig(
    contractAddress: Address,
    server: WalletClient,
    client: WalletClient,
    serverBalance: bigint,
    clientBalance: bigint,
    inflightBalance: bigint = 0n,
    sequence: bigint = 0n
  ) {

    // Prepare the signature
    // Define the EIP-712 Domain and Types
    const domain = {
      name: "EVMBitstream", // Name of your DApp
      version: "1",   // Version
      chainId: client.chain?.id,
      verifyingContract: contractAddress,
    };

    // Define the structure of the typed data
    const types = {
      ChannelClose: [
        { name: "channelId", type: "bytes32" },
        { name: "serverAmount", type: "uint256" },
        { name: "clientAmount", type: "uint256" },
        { name: "htlc", type: "Htlc" },
        { name: "sequence", type: "uint256" },
      ],
      Htlc: [
        { name: "inflightAmount", type: "uint256" },
        { name: "lockedUntil", type: "uint256" },
        { name: "secret", type: "bytes32" },
      ],
    };

    const typedData = {
      domain: domain,
      types: types,
      primaryType: "ChannelClose",
      message: {
        channelId: channelId1,
        serverAmount: serverBalance,
        clientAmount: clientBalance,
        inflightAmount: inflightBalance,
        sequence: sequence,
      },
    };

    console.log("Typed Data", typedData);

    // Must be cast to any as TS type system can not match it otherwise to signTypedData
    const clientSignature = await client.signTypedData(typedData as any);
    const serverSignature = await server.signTypedData(typedData as any);

    return {
      serverSignature,
      clientSignature,
    };
  }

  describe("constructor", function () {
    it("Should create the PaymentChannel contract", async function () {
      const { sut } = await loadFixture(deployFixture);

      expect(sut.address).to.be.not.equal("0x0");
    });
  });

  xdescribe("createChannel", function () {

    it("Creates a channel and transfers ERC20 amount into custody", async function () {
      const { sut, client, server, erc20 } = await loadFixture(deployFixture);

      await erc20.write.approve([sut.address, 10n], { account: client.account });

      await sut.write.createChannel([
        channelId1,
        server.account.address,
        client.account.address,
        erc20.address,
        10n
      ], { account: client.account });

      /* Broken as we can not easily use hardhat-viem chai matchers.
      await expect(sut.write.createChannel([
        channelId1,
        server.account.address,
        client.account.address,
        erc20.address,
        10n
      ], { account: client.account })).to.emit(sut, "ChannelCreated")
        .withArgs(channelId1, server.account.address, client.account.address, erc20.address, 10n);*/

      expect(await erc20.read.balanceOf([client.account.address])).to.eq(9_990n);
      expect(await erc20.read.balanceOf([sut.address])).to.eq(10n);
    });

    describe("Validations", function () {
      it("Should revert if server and client are equal", async function () {
        const { sut, server, erc20 } = await loadFixture(deployFixture);

        await expect(sut.write.createChannel([
          channelId1,
          server.account.address,
          server.account.address,
          erc20.address,
          10n
        ])).to.be.rejectedWith("InvalidParticipants");
      });

      it("Should revert if server is 0x0 address", async function () {
        const { sut, server, erc20 } = await loadFixture(deployFixture);

        await expect(sut.write.createChannel([
          channelId1,
          zeroAddress,
          server.account.address,
          erc20.address,
          10n
        ])).to.be.rejectedWith("InvalidParticipants");
      });

      it("Should revert if client is 0x0 address", async function () {
        const { sut, server, erc20 } = await loadFixture(deployFixture);

        await expect(sut.write.createChannel([
          channelId1,
          server.account.address,
          zeroAddress,
          erc20.address,
          10n
        ])).to.be.rejectedWith("InvalidParticipants");
      });

      it("Should revert if amount is zero", async function () {
        const { sut, server, client, erc20 } = await loadFixture(deployFixture);

        await expect(sut.write.createChannel([
          channelId1,
          server.account.address,
          client.account.address,
          erc20.address,
          0n
        ])).to.be.rejectedWith("InvalidAmount");
      });

      it("Should revert if amount is negative", async function () {
        const { sut, server, client, erc20 } = await loadFixture(deployFixture);

        await expect(sut.write.createChannel([
          channelId1,
          server.account.address,
          client.account.address,
          erc20.address,
          -10n
        ])).to.be.rejected;
      });

      it("Should revert if channel already exists", async function () {
        const { sut, server, client, erc20 } = await loadFixture(deployFixture);

        await erc20.write.approve([sut.address, 100n], { account: client.account });

        await sut.write.createChannel([
          channelId1,
          server.account.address,
          client.account.address,
          erc20.address,
          10n
        ]);

        await expect(sut.write.createChannel([
          channelId1,
          server.account.address,
          client.account.address,
          erc20.address,
          10n
        ])).to.be.rejectedWith("ChannelDoesExist");
      });

      it("Should revert if ERC20 allowance is too low", async function () {
        const { sut, server, client, erc20 } = await loadFixture(deployFixture);

        await erc20.write.approve([sut.address, 10n], { account: client.account });

        await expect(sut.write.createChannel([
          channelId1,
          server.account.address,
          client.account.address,
          erc20.address,
          20n
        ])).to.be.rejectedWith("MissingAllowance");
      });
    });

    xdescribe("Events", function () {
      it("Should emit event on channel creation", async function () {
        // TODO we can not check this as chai matcher wont work with viem.
      });
    });
  });

  describe("closeChannel", function () {

    it("closes the channel when client requests it and signatures are valid", async function () {
      const { sut, erc20, client, server } = await loadFixture(deployFixture);

      await createChannel(
        sut,
        erc20,
        server.account,
        client.account,
        100n
      );

      const { serverSignature, clientSignature } = await createChannelCloseSig(
        sut.address,
        server,
        client,
        30n,
        70n
      );

      await sut.write.closeChannel([channelId1, 30n, 70n, serverSignature, clientSignature], { account: client.account });

      // TODO check for the ChannelClose event when chai matchers work with viem
    });

    it("closes the channel when server requests it and signatures are valid", async function () {
      const { sut, erc20, client, server } = await loadFixture(deployFixture);

      await createChannel(
        sut,
        erc20,
        server.account,
        client.account,
        100n
      );

      const { serverSignature, clientSignature } = await createChannelCloseSig(
        sut.address,
        server,
        client,
        30n,
        70n
      );

      await sut.write.closeChannel([channelId1, 30n, 70n, serverSignature, clientSignature], { account: server.account });

      // TODO check for the ChannelClose event when chai matchers work with viem
    });

    it("pays out the agreed token balance", async function () {
      const { sut, erc20, client, server } = await loadFixture(deployFixture);

      await createChannel(
        sut,
        erc20,
        server.account,
        client.account,
        100n
      );

      const { serverSignature, clientSignature } = await createChannelCloseSig(
        sut.address,
        server,
        client,
        30n,
        70n
      );

      await sut.write.closeChannel([channelId1, 30n, 70n, serverSignature, clientSignature], { account: server.account });

      expect(await erc20.read.balanceOf([client.account.address])).to.eq(9_970n);
      expect(await erc20.read.balanceOf([server.account.address])).to.eq(30n);
      expect(await erc20.read.balanceOf([sut.address])).to.eq(0n);
    });

    xdescribe("Validations", function () {
      it("Reverts if channel does not exist", async function () {

      });

      it("Reverts if client signature is invalid", async function () {

      });

      it("Reverts if server signature is invalid", async function () {

      });

      it("Reverts if server and client amount does not match channel balance", async function () {

      });

      it("Reverts if called from neither server nor client", async function () {

      });
    });

    xdescribe("Events", function () {
      // TODO we can not check this as chai matcher wont work with viem.
    });
  });

  describe("forceCloseChannel", function () {

    it("force closes the channel when client requests it and signatures are valid", async function () {
      const { sut, erc20, client, server } = await loadFixture(deployFixture);

      await createChannel(
        sut,
        erc20,
        server.account,
        client.account,
        100n
      );

      const { serverSignature, clientSignature } = await createChannelCloseSig(
        sut.address,
        server,
        client,
        20n,
        70n,
        10n,
        3n
      );

      await sut.write.forceCloseChannel([{
        channelId: channelId1,
        serverAmount: 20n,
        clientAmount: 70n,
        inflightAmount: 10n,
        sequence: 3n
      }, clientSignature, serverSignature], { account: server.account });

      // No payments should have happened yet.
      expect(await erc20.read.balanceOf([client.account.address])).to.eq(9_900n);
      expect(await erc20.read.balanceOf([server.account.address])).to.eq(0n);
      expect(await erc20.read.balanceOf([sut.address])).to.eq(100n);
    });

    xdescribe("Validations", function () {
      it("Reverts if channel does not exist", async function () {

      });

      it("Reverts if called from neither server nor client", async function () {

      });

      it("Reverts if client signature is invalid", async function () {

      });

      it("Reverts if server signature is invalid", async function () {

      });

      it("Reverts if server and client amount does not match channel balance", async function () {

      });

      it("Reverts if channel is already in foced closure state", async function () {

      });
    });
  });
});
