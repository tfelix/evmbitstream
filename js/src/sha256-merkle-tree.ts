import { sha256 } from '@noble/hashes/sha256';

export class Sha256MerkleTree implements MerkleTree {

  constructor() {
  }

  getRoot(leafs: Uint8Array[]): Uint8Array {
    // Hash all the leafs first.
    let mutableLeafs = leafs.map(leaf => sha256(leaf));

    // Make sure to at least hash once even if you only start with one leaf.
    if (mutableLeafs.length % 2 !== 0) {
      mutableLeafs.push(mutableLeafs[mutableLeafs.length - 1]);
    }

    while (mutableLeafs.length > 1) {
      if (mutableLeafs.length % 2 !== 0) {
        mutableLeafs.push(mutableLeafs[mutableLeafs.length - 1]);
      }

      const newLeaves = Array.from({ length: mutableLeafs.length / 2 }, (_, i) => {
        const combinedData = Buffer.concat([mutableLeafs[i * 2], mutableLeafs[i * 2 + 1]]);

        return sha256(combinedData);
      });

      mutableLeafs = newLeaves;
    }

    return mutableLeafs[0];
  }
}