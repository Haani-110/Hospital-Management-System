/**
 * Session token store.
 *
 * Kept in a standalone module so the API client and the auth provider can share
 * it without importing each other.
 *
 * Storage strategy: the access token lives in memory for the lifetime of the tab
 * and is mirrored into `sessionStorage` so a page reload does not sign the user
 * out. `sessionStorage` is scoped to the tab and cleared when it closes, which
 * keeps the token off disk across browser restarts.
 *
 * Whenever `sessionStorage` is readable it is the source of truth, so every part
 * of the running app always sees the same session. The in-memory mirror lives in
 * a single global slot instead of module variables: duplicated module copies
 * (a dev-server hot update, a re-imported chunk) would otherwise each keep their
 * own copy of the token, and a copy that still holds `null` makes a signed-in
 * app send unauthenticated requests — which the API answers with 401 and the app
 * answers with an immediate, unexplained return to the sign-in screen. The slot
 * is only consulted when storage cannot be read at all (private mode, blocked
 * third-party storage).
 *
 * Note: the backend issues bearer tokens only (there is no cookie/refresh
 * endpoint), so an HttpOnly cookie cannot be used without a backend change.
 * Any token readable by JavaScript is exposed to XSS — the mitigations here are
 * the short-lived token, tab-scoped storage, and no third-party scripts.
 */
const STORAGE_KEY = 'hms.auth.token';
const SLOT_KEY = '__hmsAuthTokenSlot__';

interface TokenSlot {
  value: string | null;
}

type GlobalWithTokenSlot = typeof globalThis & { [SLOT_KEY]?: TokenSlot };

/** The one in-memory mirror shared by every copy of this module in the page. */
function tokenSlot(): TokenSlot {
  const globalScope = globalThis as GlobalWithTokenSlot;
  const existing = globalScope[SLOT_KEY];
  if (existing) return existing;

  const created: TokenSlot = { value: null };
  globalScope[SLOT_KEY] = created;
  return created;
}

function readStorage(): { readable: boolean; value: string | null } {
  try {
    return { readable: true, value: window.sessionStorage.getItem(STORAGE_KEY) };
  } catch {
    return { readable: false, value: null };
  }
}

export function getToken(): string | null {
  const stored = readStorage();
  if (stored.readable) {
    tokenSlot().value = stored.value;
    return stored.value;
  }
  return tokenSlot().value;
}

export function setToken(token: string | null): void {
  tokenSlot().value = token;
  try {
    if (token) {
      window.sessionStorage.setItem(STORAGE_KEY, token);
    } else {
      window.sessionStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // Storage can be unavailable (private mode, blocked cookies) — the token
    // still works for the current page load from the shared slot.
  }
}

export function clearToken(): void {
  setToken(null);
}
