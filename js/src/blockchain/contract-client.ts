import { Address, Hex } from "viem";

export type Preimage = Hex;

export interface ContractClientV1 {
  waitForReveal(): Promise<Preimage>;
  pay(receiver: Address, amount: string, token: Address): Promise<void>;
}

export class MockContractClient implements ContractClientV1 {
  private revealPromise: Preimage | null = null;

  async pay(receiver: Address, amount: string, token: Address): Promise<void> {
    return;
  }

  waitForReveal(): Promise<Preimage> {
    return Promise.resolve(this.revealPromise!);
  }

  resolveWith(preimage: Preimage): void {
    this.revealPromise = preimage;
  }
}