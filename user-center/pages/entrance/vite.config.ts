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
  // would resolve against the security page route and 404.
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    // A different port from the admin app (5173) so both dev servers
    // can run side by side.
    port: 5174,
    host: '0.0.0.0',
  },
  build: {
    outDir: 'dist',
    // Emit hashed assets under `assets/entrance/` so this app and the
    // sibling `admin` app never collide in the shared Spring static
    // root: each owns one subdirectory under `/assets/`, both still
    // covered by the framework's `/assets/**` permitAll rule. build.sh
    // therefore only ever clears its own subdirectory, never the whole
    // static root.
    assetsDir: 'assets/entrance',
    sourcemap: false,
  },
});
