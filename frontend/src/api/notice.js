import request from './request'

export function getNoticeList() {
  return request({
    url: '/api/notice/list',
    method: 'get'
  })
}

export function getNoticeById(id) {
  return request({
    url: `/api/notice/${id}`,
    method: 'get'
  })
}