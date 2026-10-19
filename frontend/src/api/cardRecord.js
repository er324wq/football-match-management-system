import request from './request'

export function getCardRecords(params) {
  return request({
    url: '/api/card-record/list',
    method: 'get',
    params
  })
}

export function createCardRecord(data) {
  return request({
    url: '/api/card-record',
    method: 'post',
    data
  })
}

export function updateCardRecord(data) {
  return request({
    url: '/api/card-record',
    method: 'put',
    data
  })
}

export function deleteCardRecord(id) {
  return request({
    url: `/api/card-record/${id}`,
    method: 'delete'
  })
}
