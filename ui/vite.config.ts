import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8088'
  const sharedProxy = {
    target: apiTarget,
    changeOrigin: true,
    secure: false,
  }

  function manualChunks(id: string) {
    if (!id.includes('node_modules')) {
      return undefined
    }

    if (/[\\/]node_modules[\\/](react-map-gl|maplibre-gl)[\\/]/.test(id)) {
      return 'maps-vendor'
    }

    if (
      /[\\/]node_modules[\\/](react-aria-components|@react-aria|@react-stately|@react-types|@internationalized|@floating-ui)[\\/]/.test(
        id,
      )
    ) {
      return 'aria-vendor'
    }

    if (/[\\/]node_modules[\\/](react|react-dom|react-router|react-router-dom|scheduler)[\\/]/.test(id)) {
      return 'react-vendor'
    }

    return undefined
  }

  return {
    plugins: [react()],
    build: {
      rollupOptions: {
        output: {
          manualChunks,
        },
      },
    },
    server: {
      port: 5174,
      proxy: {
        '/api': sharedProxy,
        '/actuator': sharedProxy,
        '/swagger-ui': sharedProxy,
        '/v3': sharedProxy,
      },
    },
  }
})
