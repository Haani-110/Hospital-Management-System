import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // 0.0.0.0 so the dev server is reachable from the host/preview proxy.
    host: '0.0.0.0',
    port: 3000,
    strictPort: true,
    // The preview/reverse-proxy host is not localhost, so the host check is
    // relaxed for local development only.
    allowedHosts: true,
    /**
     * Dev-only proxy: when the app is loaded through a remote preview (or any
     * host that cannot reach the API on localhost:4000), set
     * `VITE_API_BASE_URL=/api/v1` and requests are forwarded here instead of
     * being sent from the browser to its own localhost.
     */
    proxy: {
      '/api': {
        target: 'http://localhost:4000',
        changeOrigin: false,
      },
    },
  },
  preview: {
    host: '0.0.0.0',
    port: 3000,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    restoreMocks: true,
  },
});
