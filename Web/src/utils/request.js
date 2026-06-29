import axios from 'axios'

// Create Axios Instance
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080', // Default backend server port
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request Interceptor: Attach JWT Token to every outgoing HTTP request
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('moocpass_token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response Interceptor: Secondary encapsulation of backend Result<T> wrapper
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // Backend standard response wrapper: { code, message, data, timestamp }
    if (res.code !== undefined) {
      if (res.code === 200) {
        return res.data !== undefined ? res.data : res
      } else {
        // Handle custom backend business logic error
        console.warn(`[API Business Error ${res.code}]:`, res.message)
        return Promise.reject(new Error(res.message || '请求服务异常'))
      }
    }
    return res
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        console.warn('[AUTH 401]: Token invalid or expired. Cleaning local session.')
        localStorage.removeItem('moocpass_token')
        localStorage.removeItem('moocpass_user')
        // Dispatch custom event or redirect if needed
        window.dispatchEvent(new CustomEvent('auth-unauthorized'))
      }
      return Promise.reject(new Error(data?.message || `HTTP 错误 ${status}`))
    }
    return Promise.reject(error)
  }
)

export default request
