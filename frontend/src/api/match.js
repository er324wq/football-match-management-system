import request from './index'

export function getMatchList(params) {
  return request({
    url: '/api/match/list',
    method: 'get',
    params
  })
}

export function getAllMatches() {
  return request({
    url: '/api/match/all',
    method: 'get'
  })
}

export function addMatch(data) {
  return request({
    url: '/api/match',
    method: 'post',
    data
  })
}

export function updateMatch(data) {
  return request({
    url: '/api/match',
    method: 'put',
    data
  })
}

export function deleteMatch(id) {
  return request({
    url: `/api/match/${id}`,
    method: 'delete'
  })
}
