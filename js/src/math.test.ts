import { add } from "./math";

describe("Math functions", () => {

  it("should add 5 by 3", () => {
    const result = add(5, 3);
    expect(result).toEqual(8);
  });
});