import assert from 'node:assert/strict'
import test from 'node:test'
import axios from 'axios'
import { resolveApiBaseUrl } from '../src/apiBaseUrl.js'

test('uses a local API fallback only when supplied by the development environment', () => {
  assert.equal(
    resolveApiBaseUrl(undefined, 'http://localhost:8080/api/v1'),
    'http://localhost:8080/api/v1',
  )
})

test('rejects a missing production API URL instead of falling back to localhost', () => {
  assert.throws(() => resolveApiBaseUrl(undefined), /VITE_API_BASE_URL is required in production/)
})

test('adds the API prefix to a production backend origin', () => {
  assert.equal(
    resolveApiBaseUrl('https://campus-shield-backend-2gp1.onrender.com'),
    'https://campus-shield-backend-2gp1.onrender.com/api/v1',
  )
})

test('composes the expected production login endpoint', () => {
  const baseURL = resolveApiBaseUrl('https://campus-shield-backend-2gp1.onrender.com/api/v1')

  assert.equal(
    axios.getUri({ baseURL, url: '/auth/login' }),
    'https://campus-shield-backend-2gp1.onrender.com/api/v1/auth/login',
  )
})

test('normalizes a trailing slash on the configured API URL', () => {
  assert.equal(
    resolveApiBaseUrl('https://campus-shield-backend-2gp1.onrender.com/api/v1/'),
    'https://campus-shield-backend-2gp1.onrender.com/api/v1',
  )
})

test('does not append a duplicate API prefix', () => {
  assert.equal(
    resolveApiBaseUrl('https://campus-shield-backend-2gp1.onrender.com/api/v1'),
    'https://campus-shield-backend-2gp1.onrender.com/api/v1',
  )
  assert.throws(
    () => resolveApiBaseUrl('https://campus-shield-backend-2gp1.onrender.com/api/v1/api/v1'),
    /must not repeat/,
  )
})

test('rejects insecure production URLs and credential-bearing URLs', () => {
  assert.throws(() => resolveApiBaseUrl('http://api.example.test/api/v1'), /must use HTTPS in production/)
  assert.throws(
    () => resolveApiBaseUrl('https://user:password@api.example.test/api/v1'),
    /without credentials/,
  )
})
