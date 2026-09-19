/**
 * Session token store.
 *
 * Kept in a standalone module so the API client and the auth provider can share
 * it without importing each other.
 *
 * Storage strategy: the access token lives in memory for the lifetime of the
 * tab and is mirrored into `sessionStorage` so a page reload does not sign the
 * user out. `sessionStorage` is scoped to the tab and cleared when it closes,
 * which keeps the token off disk across browser restarts.
 *
 * Note: the backend issues bearer tokens only (there is no cookie/refresh
 * endpoint), so an HttpOnly cookie cannot be used without a backend change.
 * Any token readable by JavaScript is exposed to XSS — the mitigations here are
 * the short-lived token, tab-scoped storage, and no third-party scripts.
 */
const STORAGE_KEY = 'hms.auth.token';

let inMemoryToken: string | null = null;
let hydrated = false;

function readStorage(): string | null {
  try {
    return window.sessionStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

export function getToken(): string | null {
  if (!hydrated) {
    inMemoryToken = readStorage();
    hydrated = true;
  }
  return inMemoryToken;
}

export function setToken(token: string | null): void {
  inMemoryToken = token;
  hydrated = true;
  try {
    if (token) {
      window.sessionStorage.setItem(STORAGE_KEY, token);
    } else {
      window.sessionStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // Storage can be unavailable (private mode, blocked cookies) — the token
    // still works for the current page load from memory.
  }
}

export function clearToken(): void {
  setToken(null);
}
