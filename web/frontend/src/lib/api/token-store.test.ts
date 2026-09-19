import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { clearToken, getToken, setToken } from './token-store';

const STORAGE_KEY = 'hms.auth.token';

const originalDescriptors = {
  sessionStorage: Object.getOwnPropertyDescriptor(window, 'sessionStorage'),
};

/** Makes every access to `window.sessionStorage` throw, like a blocked iframe. */
function blockSessionStorage() {
  Object.defineProperty(window, 'sessionStorage', {
    configurable: true,
    get() {
      throw new DOMException('Access is denied for this document.', 'SecurityError');
    },
  });
}

afterEach(() => {
  if (originalDescriptors.sessionStorage) {
    Object.defineProperty(window, 'sessionStorage', originalDescriptors.sessionStorage);
  }
});

beforeEach(() => {
  window.sessionStorage.clear();
  clearToken();
});

describe('token store', () => {
  it('keeps the token in sessionStorage so a reload still has it', () => {
    setToken('jwt-token');

    expect(getToken()).toBe('jwt-token');
    expect(window.sessionStorage.getItem(STORAGE_KEY)).toBe('jwt-token');

    clearToken();

    expect(getToken()).toBeNull();
    expect(window.sessionStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('treats sessionStorage as the source of truth once it is readable', () => {
    setToken('jwt-token');

    // Another context cleared the stored session: the app must not keep signing
    // requests with a token that is no longer the session's token.
    window.sessionStorage.removeItem(STORAGE_KEY);

    expect(getToken()).toBeNull();
  });

  it('shares the in-memory token with a second copy of the module', async () => {
    // A duplicated module copy is what a dev-server hot update produces. With
    // no readable storage, both copies have to agree on the current session —
    // otherwise the app is signed in while its requests carry no token, the API
    // answers 401, and the user is thrown back to the sign-in screen.
    blockSessionStorage();
    const first = await import('./token-store');
    first.setToken('jwt-token');

    vi.resetModules();
    const second = await import('./token-store');

    expect(second.getToken()).toBe('jwt-token');
    expect(second.getToken()).toBe(first.getToken());
  });

  it('survives unavailable storage without throwing', () => {
    blockSessionStorage();

    expect(() => {
      setToken('jwt-token');
    }).not.toThrow();
    expect(getToken()).toBe('jwt-token');
    expect(() => {
      clearToken();
    }).not.toThrow();
    expect(getToken()).toBeNull();
  });
});
