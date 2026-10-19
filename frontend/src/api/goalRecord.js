import request from './request'

export function createGoalRecord(data) {
  return request({
    url: '/api/goal-record',
    method: 'post',
    data
  })
}

export function getGoalRecords(params) {
  return request({
    url: '/api/goal-record/list',
    method: 'get',
    params
  })
}

export function getGoalRecordById(id) {
  return request({
    url: `/api/goal-record/${id}`,
    method: 'get'
  })
}

export function updateGoalRecord(id, data) {
  return request({
    url: '/api/goal-record',
    method: 'put',
    data
  })
}

export function deleteGoalRecord(id) {
  return request({
    url: `/api/goal-record/${id}`,
    method: 'delete'
  })
}
