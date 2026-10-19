import request from './request'

export function login(data) {
  // 实际API调用
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

export function logout() {
  // 实际API调用
  return request({
    url: '/api/auth/logout',
    method: 'post'
  })
}