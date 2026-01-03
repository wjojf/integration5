import { createRoot } from "react-dom/client"
import { BrowserRouter } from "react-router-dom"
import { ReactKeycloakProvider } from '@react-keycloak/web'
import { QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'sonner'

import App from "./App"
import "./styles/globals.css"
import { securityClient, initOptions } from './lib/app/security.client'
import { queryClient } from './lib/app/query.client'

const rootElement = document.getElementById("root")
if (!rootElement) throw new Error('Failed to find the root element')

createRoot(rootElement).render(
  <ReactKeycloakProvider authClient={securityClient} initOptions={initOptions} >
      <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <App />
            <Toaster position="top-right" richColors />
          </BrowserRouter>
      </QueryClientProvider>
  </ReactKeycloakProvider>
)

