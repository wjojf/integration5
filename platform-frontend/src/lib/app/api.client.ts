import axios, { AxiosInstance } from 'axios'
import { API_CONFIG } from '../../config/api.config'
import Keycloak from "keycloak-js"

type KeycloakInstance = InstanceType<typeof Keycloak>

export class ApiClient {
    private static keycloak: KeycloakInstance;

    static setKeycloakInstance(keycloak: KeycloakInstance) {
        this.keycloak = keycloak;
    }

    static init(baseUrl: string = API_CONFIG.BASE_URL, timeout: number = API_CONFIG.TIMEOUT): AxiosInstance {
        const axiosInstance = axios.create({
            baseURL: baseUrl,
            timeout,
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
            },
        });

        axiosInstance.interceptors.request.use((config) => {
            const token = this.keycloak?.token;
            if (token && !config.headers['X-Skip-Auth']) {
                config.headers.Authorization = `Bearer ${token}`;
            }

            return config;
        });

        axiosInstance.interceptors.response.use(
            (response) => response,
            (error) => {
                if (error.response?.status === 401) {
                    this.keycloak?.logout({ redirectUri: window.location.origin });
                }
                return Promise.reject(error);
            }
        );

        return axiosInstance;
    }
}
