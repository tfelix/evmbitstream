import { bytesToHex } from 'viem';
import { Sha256MerkleTree } from './sha256-merkle-tree';

describe("Sha256MerkleTree", () => {

  const tree = new Sha256MerkleTree();

  describe("with single leaf", () => {

    it("returns equal results as the server implementation", () => {
      const leaf = [Uint8Array.from([0x1])];

      const result = tree.getRoot(leaf);

      expect(bytesToHex(result))
        .toEqual('0xe28c8b26b936e24632469d468079a29f00a3325a104a013a21dc744d2ec35129');
    });
  });

  describe("with multiple leafs", () => {
    it("returns equal results as the server implementation", () => {
      const leafs = [
        Uint8Array.from([0x1]),
        Uint8Array.from([0x2]),
        Uint8Array.from([0x3])
      ];

      const result = tree.getRoot(leafs);

      expect(bytesToHex(result))
        .toEqual('0x9faa2a58b06fa09e3df6f260fcd26040b798fd90bfb33a759f85ef29e95ae648');
    });
  });

});