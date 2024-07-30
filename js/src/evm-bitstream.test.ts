import { HttpClientV1 } from './http/http-client-v1';
import { EvmBitstream } from './evm-bitstream';
import { Sha256MerkleTree } from './sha256-merkle-tree';
import { UrlBuilderV1 } from './http/url-builder-v1';
import { MockContractClient } from './blockchain/contract-client';
import { b64toBlob } from './utils/b64-to-blob';

// Hello Bitstream!
const encryptedFile = "exprT/Y+tj2Mq6tcOS5i+7v2vW4sPBVw3oVvflg4Ufn6QFBoQ2zeQhIVtLoFGt/w";
const expectedPublicAddress = "0x5b38da6a701c568545dcfcb03fcb875f56beddc4";
const expectedPreimage = "0x68901504f8affca1b02e9ba16d661a64eb15b194778fbc4e8a7500d124cec172";
const expectedPreimageHash = "0xdb6fac6f5a2a80ee6e17d042ac7757683b28e5df1bd146ca8173206688c7e641";

const httpClientGetBitstreamMock = jest
  .spyOn(HttpClientV1.prototype, 'getBitstream')
  .mockImplementation(() => {
    return Promise.resolve({
      paymentAmount: "1000000",
      paymentHash: "0xdb6fac6f5a2a80ee6e17d042ac7757683b28e5df1bd146ca8173206688c7e641",
      filename: "test.txt",
      mime: "text/plain",
      data: b64toBlob(encryptedFile),
      signature: "0x360bd2051ef318e9f963e9ed992c91e8a6c21d61c26a06f6d045ef7804f60a7972e8778872a30a5f52d4dacc2271c0a08d407d4ea0e1ffc66f6ce070c1e34c931c"
    });
  }); // comment this line if just want to "spy"

describe("EvmBitstream", () => {

  const contractClient = new MockContractClient();
  const sut = new EvmBitstream(
    new HttpClientV1(new UrlBuilderV1()),
    contractClient,
    new Sha256MerkleTree(),
  );

  fdescribe("fetch with cooperative server", () => {

    it("decrypts the file successfull", async () => {
      const result = sut.fetch('0xbb8ece46d0814b21d57f0317624be680c335f6c6d344e06d3ea466526221e7e8');
      contractClient.resolveWith(expectedPreimage);

      result.finally(() => {
        console.log('geht');
      })
    });
  });
});