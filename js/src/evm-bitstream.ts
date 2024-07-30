import { bytesToHex, hexToBytes, hexToString, recoverMessageAddress } from 'viem';
import { sha256 } from '@noble/hashes/sha256';

import { HttpClientV1 } from './http/http-client-v1';
import { BitstreamException, ErrorCodes } from './bitstream-exception';
import { ContractClientV1 } from './blockchain/contract-client';
import { Decryptor } from './bitstream/decryptor';

export class EvmBitstream {

  constructor(
    private readonly httpClient: HttpClientV1,
    private readonly contractClient: ContractClientV1,
    private readonly merkleTree: MerkleTree
  ) { }

  private decryptor = new Decryptor();

  public async fetch(fileId: string): Promise<void> {
    const bitstream = await this.httpClient.getBitstream(fileId);

    // TODO consider not equally sized chunks for better efficiency.
    const chunks = await this.blobToChunks(bitstream.data, 32);

    const hashedChunks = chunks.filter((_, index) => index % 2 == 0);
    const encryptedChunks = chunks.filter((_, index) => (index + 1) % 2 == 0);
    const encId = this.merkleTree.getRoot(chunks);
    const resultFileId = bytesToHex(this.merkleTree.getRoot(hashedChunks));

    if (encryptedChunks.length != hashedChunks.length) {
      throw new BitstreamException(`Unequal length of chunks`, ErrorCodes.UNEQUAL_CHUNK_LENGTH);
    }

    /* TODO FIX ME
    if (fileId != resultFileId) {
      throw new BitstreamException(`Server delivered fileId ${resultFileId}, but expected ${fileId}`, ErrorCodes.INVALID_FILE_ID);
    }*/

    const claim = Buffer.concat([encId, hexToBytes(bitstream.paymentHash)]);
    const address = await recoverMessageAddress({
      message: { raw: claim },
      signature: bitstream.signature,
    });

    // TODO verify if this address is also the address associated with the server and if bond is there.
    // TODO verify the server bond validity for his address

    // Pay the requested token amount when all is okay
    await this.contractClient.pay(address, bitstream.paymentAmount, '0xNOTSET');

    // Listen to the server collecting payment via revealed preimage
    const preimage = await this.contractClient.waitForReveal();

    // TODO handle failed decryption to slash server.
    // Decrypt file with preimage
    const decryptedFile = this.decryptor.decryptFile(preimage, encryptedChunks, hashedChunks);

    // TODO validate the decryption
    // TODO If file OK: return a file.
    // TODO If file not ok: prepare proof and slash server
  }

  private async blobToChunks(blob: Blob, chunkSize: number): Promise<Uint8Array[]> {
    const arrayBuffer = await blob.arrayBuffer();
    const byteArray = new Uint8Array(arrayBuffer);
    const byteArrays: Uint8Array[] = [];

    for (let offset = 0; offset < byteArray.length; offset += chunkSize) {
      const chunk = byteArray.slice(offset, offset + chunkSize);
      byteArrays.push(chunk);
    }

    return byteArrays;
  }
}