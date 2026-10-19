import request from './request'

export function getUserNotifications(userId) {
  return request({
    url: '/api/notification/user',
    method: 'get',
    params: { userId }
  })
}

export function getUnreadCount(userId) {
  return request({
    url: '/api/notification/unread-count',
    method: 'get',
    params: { userId }
  })
}

export function markAsRead(id) {
  return request({
    url: `/api/notification/read/${id}`,
    method: 'put'
  })
}

export function markAllAsRead(userId) {
  return request({
    url: '/api/notification/read-all',
    method: 'put',
    params: { userId }
  })
}
