import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    // Output directly into the Spring Boot static resources folder
    outDir: '../clamped-server/src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      // During dev, proxy /api calls to the Spring Boot server
      '/api': 'http://localhost:8080',
    },
  },
})
