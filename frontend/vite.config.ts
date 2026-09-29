import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'node:path';

export default defineConfig(({ mode }) => {
  // O front local fala com a API da VPS (o banco é sempre o dela). O navegador chama /api na própria
  // máquina e o Vite repassa: a VPS só libera CORS para o domínio de produção, então o cabeçalho
  // Origin é removido no repasse. Para usar um backend local, ponha VITE_API_URL em .env.development.local.
  const env = loadEnv(mode, process.cwd(), '');
  const target = env.VITE_PROXY_TARGET || 'https://piads2026-rh.duckdns.org';

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': path.resolve(import.meta.dirname, './src'),
      },
    },
    server: {
      proxy: {
        '/api': {
          target,
          changeOrigin: true,
          secure: true,
          configure: (proxy) => proxy.on('proxyReq', (request) => request.removeHeader('origin')),
        },
      },
    },
  };
});
