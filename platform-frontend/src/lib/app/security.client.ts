import Keycloak from "keycloak-js";

export const securityClient = new Keycloak({
    url: import.meta.env.VITE_KC_URL,
    realm: import.meta.env.VITE_KC_REALM,
    clientId: import.meta.env.VITE_KC_CLIENT_ID
});

export const initOptions = {
    onLoad: 'check-sso' as const,
    flow: 'standard' as const,
    pkceMethod: 'S256' as const,
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    checkLoginIframe: true,
    checkLoginIframeInterval: 30
};

