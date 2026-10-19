import request from './index'

export function getStandings() {
  return request({
    url: '/api/statistics/standings',
    method: 'get'
  })
}

export function getTopScorers() {
  return request({
    url: '/api/statistics/top-scorers',
    method: 'get'
  })
}

export function getTopAssists() {
  return request({
    url: '/api/statistics/top-assists',
    method: 'get'
  })
}

export function getPersonalPerformance(userId) {
  return request({
    url: `/api/statistics/personal/${userId}`,
    method: 'get'
  })
}
