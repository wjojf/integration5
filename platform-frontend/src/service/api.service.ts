import { ApiClient } from '../lib/app/api.client'
import {AxiosInstance, AxiosRequestConfig} from "axios";

export class ApiService {
    private axios: AxiosInstance = ApiClient.init()

    /**
     * @protected
     * @param {string} endpoint
     * @param {object} [params = {}]
     * @param {AxiosRequestConfig} config
     * @returns {Promise<any>}
     * @description Handle get request.
     */
    protected async get<T>(endpoint: string, params: Record<string, any> = {}, config?: AxiosRequestConfig): Promise<T> {

        return this.request<T>(endpoint, {method: 'GET', params, ...config});
    }

    /**
     * @protected
     * @param {string} endpoint
     * @param {object} [data]
     * @param {AxiosRequestConfig} config
     * @returns {Promise<any>}
     * @description Handle post request.
     */
    protected async post<T>(endpoint: string, data?: object, config?: AxiosRequestConfig): Promise<T> {

        return this.request<T>(endpoint, {method: 'POST', data, ...config,});
    }

    /**
     * @protected
     * @param {string} endpoint
     * @param {object} [data]
     * @param {AxiosRequestConfig} [config]
     * @returns {Promise<any>}
     * @description Handle put request.
     */
    protected async put<T>(endpoint: string, data?: object, config?: AxiosRequestConfig): Promise<T> {

        return this.request<T>(endpoint, { method: 'PUT', data, ...config });
    }

    /**
     * @protected
     * @param {string} endpoint
     * @param {object} [data]
     * @param {AxiosRequestConfig} [config]
     * @returns {Promise<any>}
     * @description Handle path request.
     */
    protected async patch<T>(endpoint: string, data?: object, config?: AxiosRequestConfig): Promise<T> {

        return this.request<T>(endpoint, { method: 'PATCH', data, ...config });
    }

    /**
     * @protected
     * @param {string} endpoint
     * @param {AxiosRequestConfig} [config]
     * @returns {Promise<any>}
     * @description Handle delete request.
     */
    protected async delete<T>(endpoint: string, config?: AxiosRequestConfig): Promise<T> {

        return this.request<T>(endpoint, {method: 'DELETE', ...config});
    }

    /**
     * @protected
     * @param {string} endpoint
     * @param {AxiosRequestConfig} [config={}]
     * @returns {object}
     * @description Handle api request.
     */
    protected async request<T>(endpoint: string, config: AxiosRequestConfig = {}): Promise<T> {
        const response = await this.axios.request<T>({url: endpoint, ...config});

        return response.data;
    }
}
