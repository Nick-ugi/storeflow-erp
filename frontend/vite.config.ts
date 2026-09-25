import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    // 공통 vendor 청크(React · antd 코어 · axios, gzip 약 200KB)는 모든 화면이 함께 쓰고 캐시되므로
    // 더 쪼개도 첫 로딩량이 줄지 않는다. 화면 코드는 router/pages.ts에서 화면별로 나눈다.
    chunkSizeWarningLimit: 700,
  },
  server: {
    port: 5173,
    // 개발 중에는 Vite가 백엔드 요청을 프록시하므로 CORS 설정이 필요 없다.
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
    },
  },
})
