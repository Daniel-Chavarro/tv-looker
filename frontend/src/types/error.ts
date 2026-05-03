export type Body = {
  detail: string;
  instance: string;
  status: number;
  title: string;
  type: string;
};

export type ContentDisposition = {
  attachment: boolean;
  charset: null;
  filename: null;
  formData: boolean;
  inline: boolean;
  name: null;
  type: string;
};

export type Headers = {
  ETag: null;
  accept: unknown[];
  acceptCharset: unknown[];
  acceptLanguage: unknown[];
  acceptLanguageAsLocales: unknown[];
  acceptPatch: unknown[];
  accessControlAllowCredentials: boolean;
  accessControlAllowHeaders: unknown[];
  accessControlAllowMethods: unknown[];
  accessControlAllowOrigin: null;
  accessControlExposeHeaders: unknown[];
  accessControlMaxAge: number;
  accessControlRequestHeaders: unknown[];
  accessControlRequestMethod: null;
  allow: unknown[];
  cacheControl: null;
  connection: unknown[];
  contentDisposition: ContentDisposition;
  contentLanguage: null;
  contentLength: number;
  contentType: null;
  date: number;
  empty: boolean;
  expires: number;
  host: null;
  ifMatch: unknown[];
  ifModifiedSince: number;
  ifNoneMatch: unknown[];
  ifUnmodifiedSince: number;
  lastModified: number;
  location: null;
  origin: null;
  pragma: null;
  range: unknown[];
  upgrade: null;
  vary: unknown[];
};

export type ApiError = {
  body: Body;
  detailMessageArguments: null;
  detailMessageCode: string;
  headers: Headers;
  statusCode: string;
  titleMessageCode: string;
  typeMessageCode: string;
};
