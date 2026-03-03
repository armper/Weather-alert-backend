import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8088'

  return {
    plugins: [react()],
    server: {
      port: 5174,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
        },
        '/actuator': {
          target: apiTarget,
          changeOrigin: true,
        },
        '/swagger-ui': {
          target: apiTarget,
          changeOrigin: true,
        },
        '/v3': {
          target: apiTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
