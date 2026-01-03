import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
    plugins: [react(), tailwindcss()],
    resolve: {
        extensions: ['.ts', '.tsx', '.json'],
    },
    build: {
        target: 'esnext',
        outDir: 'dist',
    },
    optimizeDeps: {
        exclude: ['sockjs-client'],
        include: ['@stomp/stompjs'],
    },
});
