import request from './request'

export function getUserApplications(userId) {
  return request({
    url: '/api/application/user',
    method: 'get',
    params: { userId }
  })
}

export function createTeamApplication(userId, teamId, reason) {
  return request({
    url: '/api/application/team',
    method: 'post',
    data: {
      userId,
      teamId,
      reason
    }
  })
}

export function createMatchApplication(userId, matchId, reason) {
  return request({
    url: '/api/application/match',
    method: 'post',
    data: {
      userId,
      matchId,
      reason
    }
  })
}

export function createMatchCreateApplication(userId, data) {
  return request({
    url: '/api/application/create-match',
    method: 'post',
    data: { userId, ...data }
  })
}

export function createTeamCreateApplication(userId, data) {
  return request({
    url: '/api/application/create-team',
    method: 'post',
    data: { userId, ...data }
  })
}

export function updateApplicationStatus(id, status) {
  return request({
    url: `/api/application/status/${id}`,
    method: 'put',
    params: {
      status
    }
  })
}

export function getAllApplications() {
  return request({
    url: '/api/application/all',
    method: 'get'
  })
}

export function getApprovedMatchEvents() {
  return request({
    url: '/api/application/match-events',
    method: 'get'
  })
}

export function getEventTeamApplications(eventId) {
  return request({
    url: `/api/application/match-event/${eventId}/teams`,
    method: 'get'
  })
}

export function getEventArrangedMatches(eventId) {
  return request({
    url: `/api/application/match-event/${eventId}/matches`,
    method: 'get'
  })
}

export function randomArrangeMatches(eventId) {
  return request({
    url: `/api/application/match-event/${eventId}/random-arrange`,
    method: 'post'
  })
}

export function getTeamMembersForLineup(eventId, teamId) {
  return request({
    url: `/api/application/match-event/${eventId}/team/${teamId}/members`,
    method: 'get'
  })
}

export function getTeamLineup(eventId, teamId) {
  return request({
    url: `/api/application/match-event/${eventId}/team/${teamId}/lineup`,
    method: 'get'
  })
}

export function saveTeamLineup(eventId, teamId, data) {
  return request({
    url: `/api/application/match-event/${eventId}/team/${teamId}/lineup`,
    method: 'post',
    data
  })
}
