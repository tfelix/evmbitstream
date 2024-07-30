import { BitstreamException, ErrorCodes } from "../bitstream-exception";

export function xor(arr1: Uint8Array, arr2: Uint8Array): Uint8Array {
  if (arr1.length > arr2.length) {
    throw new BitstreamException('XOR arrays invalid lengths', ErrorCodes.ASSERTION_VIOLATION);
  }

  const result = new Uint8Array(arr1.length);

  for (let i = 0; i < result.length; i++) {
    result[i] = arr1[i] ^ arr2[i];
  }

  return result;
}