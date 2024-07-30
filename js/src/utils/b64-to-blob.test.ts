import { b64toBlob } from "./b64-to-blob";
import { uint8Equals } from "./uint8-equals";

describe("b64ToBlob", () => {

  describe("when given a base64 encoded string", () => {

    const str = "Test";
    const b64Str = "VGVzdA==";

    it("returns the correct byte representation", async () => {
      const blob = b64toBlob(b64Str);
      const data = await blob.arrayBuffer();

      let utf8Encode = new TextEncoder();
      const strArray = utf8Encode.encode(str);

      expect(uint8Equals(new Uint8Array(data), new Uint8Array(strArray))).toBeTruthy();
    });
  });
});