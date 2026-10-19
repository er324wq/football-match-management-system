import request from './index'

export function getMatchResultList(params) {
  return request({
    url: '/api/match-result/list',
    method: 'get',
    params
  })
}

export function getMatchResultsWithMatch() {
  return request({
    url: '/api/match-result/with-match',
    method: 'get'
  })
}

export function addMatchResult(data) {
  return request({
    url: '/api/match-result',
    method: 'post',
    data
  })
}

export function updateMatchResult(data) {
  return request({
    url: '/api/match-result',
    method: 'put',
    data
  })
}

export function deleteMatchResult(id) {
  return request({
    url: `/api/match-result/${id}`,
    method: 'delete'
  })
}
