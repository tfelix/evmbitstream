import { UrlBuilder } from "./url-builder";

export interface Bitstream {
  data: Blob,
  paymentHash: string;
  signature: string;
}

export class HttpClientV1 {

  constructor(
    private readonly urlBuilder: UrlBuilder
  ) {

  }

  public async getBitstream(fileId: string): Promise<Bitstream> {
    const url = this.urlBuilder.buildDownloadUrl(fileId);

    const r = await fetch(url);
    const paymentHash = this.getHeader(r, 'X-Payment-Hash');
    const signature = this.getHeader(r, 'X-Bitstream-Signature');
    const data = await r.blob();

    return ({
      paymentHash,
      data,
      signature
    });
  }

  private getHeader(r: Response, headerName: string): string {
    return r.headers.get(headerName) ?? this.throwNPE(headerName);
  }

  private throwNPE(missingHeader: string): never {
    throw new Error(`Server response was missing required header: ${missingHeader}`);
  }
}