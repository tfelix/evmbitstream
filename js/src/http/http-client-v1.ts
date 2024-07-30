import { UrlBuilder } from "./url-builder";

export type Hex = `0x${string}`;

export interface Bitstream {
  data: Blob,
  paymentHash: Hex;
  signature: Hex;
  mime: string;
  paymentAmount: string;
  filename: string;
}

export class HttpClientV1 {

  constructor(
    private readonly urlBuilder: UrlBuilder
  ) {

  }

  public async getBitstream(fileId: string): Promise<Bitstream> {
    const url = this.urlBuilder.buildDownloadUrl(fileId);

    const r = await fetch(url);
    const paymentHash = this.getHeader(r, 'X-Payment-Hash') as Hex;
    const signature = this.getHeader(r, 'X-Bitstream-Signature') as Hex;
    const mime = this.getHeader(r, "X-Bitstream-File-Mime");
    const paymentAmount = this.getHeader(r, "X-Bitstream-Amount");
    const filename = this.getHeader(r, "X-Bitstream-File-Name");
    const data = await r.blob();

    return ({
      paymentHash,
      data,
      signature,
      mime,
      paymentAmount,
      filename
    });
  }

  private getHeader(r: Response, headerName: string): string {
    return r.headers.get(headerName) ?? this.throwNPE(headerName);
  }

  private throwNPE(missingHeader: string): never {
    throw new Error(`Server response was missing required header: ${missingHeader}`);
  }
}