export {};

declare module 'axios' {
  interface AxiosRequestConfig {
    /** Do not attach an Authorization header and never refresh on 401. */
    skipAuth?: boolean;
  }

  interface InternalAxiosRequestConfig {
    skipAuth?: boolean;
    _retry?: boolean;
  }
}
