import request from './request'

export default {
  getMetrics() {
    return request({
      url: '/api/performance/metrics',
      method: 'get'
    })
  },
  getLogs(params) {
    return request({
      url: '/api/performance/logs',
      method: 'get',
      params
    })
  },
  getBackups() {
    return request({
      url: '/api/performance/backups',
      method: 'get'
    })
  },
  createBackup(data) {
    return request({
      url: '/api/performance/backup',
      method: 'post',
      data
    })
  },
  restoreBackup(backupId, data) {
    return request({
      url: `/api/performance/restore/${backupId}`,
      method: 'post',
      data
    })
  }
}
