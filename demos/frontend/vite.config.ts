import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { BACKEND } from './demos.config'

/**
 * Vite 开发代理：浏览器只打 :5173，/api 统一转发到 demos/backend（:8080）。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      [BACKEND.proxyPrefix]: {
        target: `http://127.0.0.1:${BACKEND.port}`,
        changeOrigin: true,
        timeout: 0,
        proxyTimeout: 0,
      },
    },
  },
})
