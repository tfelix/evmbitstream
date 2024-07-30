import { bytesToHex } from "viem";
import { xor } from "./xor";

describe("Xor", () => {

  const startData = Uint8Array.from([0x1, 0x1, 0x12, 0x34, 0xFF]);
  const value = Uint8Array.from([0x5, 0x7, 0x10, 0xF1, 0x03]);

  let xoredValue: Uint8Array;

  describe("when xored", () => {
    it("returns a proper result", () => {
      xoredValue = xor(startData, value);

      expect(bytesToHex(xoredValue)).toEqual('0x040602c5fc');
    });
  });

  describe("when xored again with the same value", () => {
    it("it returns the original data", () => {
      const expectedStart = xor(xoredValue, value);

      expect(bytesToHex(expectedStart)).toEqual(bytesToHex(startData));
    });
  });

});