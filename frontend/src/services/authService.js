import axios from 'axios'

export const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const stored = localStorage.getItem('ec_auth')
  if (stored) {
    const { token } = JSON.parse(stored)
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export async function loginRequest({ email, password, tenantId }) {
  const { data } = await api.post('/auth/login', { email, password, tenantId })
  return data
}
