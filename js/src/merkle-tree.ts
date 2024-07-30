interface MerkleTree {
  getRoot(leafs: Uint8Array[]): Uint8Array;
}