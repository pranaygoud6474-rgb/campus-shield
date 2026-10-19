import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
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
