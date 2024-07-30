import { bytesToHex, hexToBytes } from "viem";
import { Preimage } from "../blockchain/contract-client";
import { xor } from "./xor";
import { sha256 } from "@noble/hashes/sha256";
import { uint8Equals } from "../utils/uint8-equals";
import { BitstreamException, ErrorCodes } from "../bitstream-exception";

export class Decryptor {

  decryptFile(
    preimage: Preimage,
    encryptedChunks: Uint8Array[],
    hashChunks: Uint8Array[]
  ): Uint8Array {
    const preimageBytes = hexToBytes(preimage);

    const decryptedChunks = encryptedChunks.map((encChunk, index) => {
      const secret = Buffer.concat([preimageBytes, this.longToUint8Array(index + 1)]);
      const secretHashed = sha256(secret);

      const hashChunk = hashChunks[index];

      const decChunk = xor(encChunk, secretHashed);

      this.requireChunkMatch(decChunk, hashChunk);

      return decChunk;
    });

    return Buffer.concat(decryptedChunks);
  }

  private longToUint8Array(i: number): Uint8Array {
    return Uint8Array.of(
      (i & 0xff00000000000000) >> 56,
      (i & 0x00ff000000000000) >> 48,
      (i & 0x0000ff0000000000) >> 40,
      (i & 0x000000ff00000000) >> 32,
      (i & 0x00000000ff000000) >> 24,
      (i & 0x0000000000ff0000) >> 16,
      (i & 0x000000000000ff00) >> 8,
      (i & 0x00000000000000ff) >> 0
    );
  }

  private requireChunkMatch(decChunk: Uint8Array, hash: Uint8Array) {
    const hashDecChunk = sha256(decChunk);
    if (!uint8Equals(hashDecChunk, hash)) {
      throw new BitstreamException(
        `Hash ${bytesToHex(hashDecChunk)} of decrypted ${bytesToHex(decChunk)} does not match ${bytesToHex(hash)}`,
        ErrorCodes.INVALID_DECRYPTION
      );
    }
  }
}