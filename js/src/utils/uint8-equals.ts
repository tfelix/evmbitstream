export function uint8Equals(a: Uint8Array, b: Uint8Array) {
  return a.length === b.length &&
    a.every((val, index) => val === b[index]);
}