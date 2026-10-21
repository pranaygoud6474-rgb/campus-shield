const API_PATH = '/api/v1'

export function resolveApiBaseUrl(configuredUrl, developmentFallback) {
  const value = configuredUrl?.trim() || developmentFallback
  if (!value) {
    throw new Error(
      'VITE_API_BASE_URL is required in production and must point to the deployed API origin or its /api/v1 base path.',
    )
  }

  let url
  try {
    url = new URL(value)
  } catch {
    throw new Error('VITE_API_BASE_URL must be a valid absolute HTTP(S) URL.')
  }

  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) {
    throw new Error('VITE_API_BASE_URL must be an HTTP(S) URL without credentials, query parameters, or fragments.')
  }

  if (!developmentFallback && url.protocol !== 'https:') {
    throw new Error('VITE_API_BASE_URL must use HTTPS in production.')
  }

  const path = url.pathname.replace(/\/+$/, '')
  if (path && !path.endsWith(API_PATH)) {
    throw new Error(`VITE_API_BASE_URL must be an origin or end with ${API_PATH}.`)
  }
  if (path.endsWith(`${API_PATH}${API_PATH}`)) {
    throw new Error(`VITE_API_BASE_URL must not repeat ${API_PATH}.`)
  }

  url.pathname = path || API_PATH
  return url.toString().replace(/\/$/, '')
}
