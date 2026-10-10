// Dev proxy: /api -> Spring Boot :8080.
// Header Origin dihapus agar request POST/PUT/DELETE tidak ditolak CORS backend (403 "Invalid CORS request"),
// karena dari sisi browser request ini sama-origin (localhost:4200) dan hanya diteruskan oleh proxy.
export default {
  '/api': {
    target: 'http://localhost:8080',
    secure: false,
    changeOrigin: true,
    configure: (proxy) => {
      proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'));
    },
  },
};
