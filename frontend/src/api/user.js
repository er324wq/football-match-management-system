import request from './index'

export function getUserList(params) {
  return request({
    url: '/api/user/list',
    method: 'get',
    params
  })
}

export function getUserById(id) {
  return request({
    url: `/api/user/${id}`,
    method: 'get'
  })
}

export function addUser(data) {
  return request({
    url: '/api/user',
    method: 'post',
    data
  })
}

export function updateUser(data) {
  return request({
    url: '/api/user',
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/api/user/${id}`,
    method: 'delete'
  })
}
