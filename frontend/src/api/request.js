import axios from 'axios'
import { Message } from 'element-ui'
import store from '../store'

// 使用相对路径，由 devServer proxy（见 config/index.js）转发到后端，避免跨域导致 OPTIONS 预检失败而出现 Network Error
const service = axios.create({
  baseURL: '/',
  timeout: 10000
})

service.interceptors.request.use(
  config => {
    const token = store.state.token
    if (token && config.url !== '/api/auth/login') {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 0) {
      return res.data
    } else {
      Message.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
  },
  error => {
    if (error.response && error.response.status === 401) {
      // 清除 token 和用户信息
      store.state.token = ''
      store.state.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      // 跳转到登录页
      if (location.hash !== '#/login') location.href = '#/login'
      Message.error('登录已过期，请重新登录')
    } else if (error.response && error.response.status === 403) {
      // 403 错误表示权限不足，不需要清除 token
      Message.error((error.response.data && error.response.data.msg) || '权限不足，请联系管理员')
    } else {
      const responseMessage = error.response && error.response.data && error.response.data.msg
      Message.error(responseMessage || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
