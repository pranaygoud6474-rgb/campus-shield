import axios from 'axios'
import { resolveApiBaseUrl } from './apiBaseUrl'

const api = axios.create({
  baseURL: resolveApiBaseUrl(
    import.meta.env.VITE_API_BASE_URL,
    import.meta.env.DEV ? 'http://localhost:8080/api/v1' : undefined,
  ),
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('campus-safety-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export function getApiError(error) {
  return error.response?.data?.message || error.message || 'The request failed. Please try again.'
}

export default api
