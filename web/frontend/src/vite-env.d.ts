/// <reference types="vite/client" />

/**
 * Environment variables exposed to the client bundle.
 *
 * Only `VITE_`-prefixed values are visible in the browser, and only the API
 * base URL is needed — no secrets are ever shipped to the frontend.
 */
interface ImportMetaEnv {
  /** Base URL of the backend API, e.g. `http://localhost:4000/api/v1`. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
