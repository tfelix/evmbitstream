import 'unfetch/polyfill';
import { UrlBuilder } from './http/url-builder';
import { HttpClientV1 } from './http/http-client-v1';

class EvmBitstream {

  constructor(
    private readonly httpClient: HttpClientV1
  ) { }

  public async fetch(fileId: string): Promise<void> {
    const bitstream = await this.httpClient.getBitstream(fileId);

    // generate encId 
    // concat encId, paymentHash
    // verify signature
    // verify fileId from hashes
  }
}