import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

/** Backend base URL for dev proxy (Spring Boot default :8080). Override: VITE_PROXY_TARGET=http://127.0.0.1:9090 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const apiTarget = env.VITE_PROXY_TARGET || "http://127.0.0.1:8080";

  const proxy = {
    target: apiTarget,
    changeOrigin: true,
  };

  return {
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: false,
      proxy: {
        "/api": proxy,
        "/users": proxy,
        "/routes": proxy,
        "/rides": proxy,
        "/bookings": proxy,
        "/reviews": proxy,
      },
    },
  };
});
