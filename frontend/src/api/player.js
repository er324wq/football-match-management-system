import request from './index'

export function getPlayerList(params) {
  return request({
    url: '/api/player/list',
    method: 'get',
    params
  })
}

export function getAllPlayers(params) {
  return request({
    url: '/api/player/all',
    method: 'get',
    params
  })
}

export function getPlayersWithTeam() {
  return request({
    url: '/api/player/with-team',
    method: 'get'
  })
}

export function addPlayer(data) {
  return request({
    url: '/api/player',
    method: 'post',
    data
  })
}

export function updatePlayer(data) {
  return request({
    url: '/api/player',
    method: 'put',
    data
  })
}

export function deletePlayer(id) {
  return request({
    url: `/api/player/${id}`,
    method: 'delete'
  })
}

export function getPlayerById(id) {
  return request({
    url: `/api/player/${id}`,
    method: 'get'
  })
}

export function getPlayersByTeam(teamId) {
  return request({
    url: `/api/player/team/${teamId}`,
    method: 'get'
  })
}
