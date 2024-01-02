import { UrlBuilder } from "./url-builder";

export class UrlBuilderV1 implements UrlBuilder {

  constructor(
    private readonly baseUrl: string = 'http://localhost:80'
  ) {
  }

  buildDownloadUrl(fileId: string): string {
    return `${this.baseUrl}/v1/download/${fileId}`;
  }

}