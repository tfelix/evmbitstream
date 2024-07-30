export enum ErrorCodes {
  /**
   * Generic error of an internal data check. Usually this is
   * a bug in the code.
   */
  ASSERTION_VIOLATION,
  INVALID_FILE_ID,
  UNEQUAL_CHUNK_LENGTH,
  INVALID_DECRYPTION
}

export class BitstreamException extends Error {
  constructor(message: string, errCode: ErrorCodes) {
    super(message);
  }
}