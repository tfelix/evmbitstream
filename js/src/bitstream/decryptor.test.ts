import { bytesToHex, hexToBytes } from "viem";
import { Decryptor } from "./decryptor";

describe("Decryptor", () => {

  const sut = new Decryptor();

  describe("when given a single chunk with a valid preimage", () => {

    // Hello Bitstream!
    const preimage = "0x68901504f8affca1b02e9ba16d661a64eb15b194778fbc4e8a7500d124cec172";
    const hashedChunk = "0x7b1a6b4ff63eb63d8cabab5c392e62fbbbf6bd6e2c3c1570de856f7e583851f9";
    const encryptedChunk = "0xfa405068436cde421215b4ba051adff0";
    const decryptedChunk = "0x48656c6c6f2042697473747265616d21";

    it("decrypts the data successfully", () => {
      const result = sut.decryptFile(preimage, [hexToBytes(encryptedChunk)], [hexToBytes(hashedChunk)]);
      const resultStr = bytesToHex(result);

      expect(resultStr).toEqual(decryptedChunk);
    });
  });
});