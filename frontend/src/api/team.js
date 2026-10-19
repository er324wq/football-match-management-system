import request from './index'

export function getTeamList(params) {
  return request({
    url: '/api/team/list',
    method: 'get',
    params
  })
}

export function getAllTeams() {
  return request({
    url: '/api/team/all',
    method: 'get'
  })
}

export function addTeam(data) {
  return request({
    url: '/api/team',
    method: 'post',
    data
  })
}

export function updateTeam(data) {
  return request({
    url: '/api/team',
    method: 'put',
    data
  })
}

export function deleteTeam(id) {
  return request({
    url: `/api/team/${id}`,
    method: 'delete'
  })
}

export function dissolveTeam(id) {
  return request({
    url: `/api/team/dissolve/${id}`,
    method: 'put'
  })
}

export function getTeamById(id) {
  return request({
    url: `/api/team/${id}`,
    method: 'get'
  })
}

export function getUserTeams(userId) {
  return request({
    url: '/api/team/user',
    method: 'get',
    params: { userId }
  })
}
