import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // No `base` override: the built index.html is served by Spring as a
  // Thymeleaf template while assets are copied to the static root, so
  // they must stay absolute (`/assets/…`). A relative `base: './'`
  // would resolve against the `/admin/console` route and 404.
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
  },
  build: {
    outDir: 'dist',
    // Emit hashed assets under `assets/admin/` so this app and the
    // sibling `entrance` app never collide in the shared Spring static
    // root: each owns one subdirectory under `/assets/`, both still
    // covered by the framework's `/assets/**` permitAll rule. build.sh
    // therefore only ever clears its own subdirectory, never the whole
    // static root.
    assetsDir: 'assets/admin',
    sourcemap: false,
  },
});
