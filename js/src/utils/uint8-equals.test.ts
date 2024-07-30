import { uint8Equals } from "./uint8-equals";

describe("uint8Equals", () => {

  describe("when given equal array", () => {
    it("returns true", async () => {
      expect(uint8Equals(new Uint8Array([0, 0, 1]), new Uint8Array([0, 0, 1])))
        .toBeTruthy();
    });
  });

  describe("when given un-equal array", () => {

    it("returns false", async () => {
      expect(uint8Equals(new Uint8Array([0, 1, 1]), new Uint8Array([0, 0, 1])))
        .toBeFalsy();
    });
  });
});