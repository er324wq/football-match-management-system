<template>
  <div class="performance-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="系统监控" name="metrics">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card class="metric-card">
              <div slot="header" class="card-header">
                <span><i class="el-icon-s-flag"></i> 系统健康状态</span>
              </div>
              <div class="metric-content">
                <el-tag :type="metrics.health === 'UP' ? 'success' : 'danger'" size="large">
                  {{ metrics.health === 'UP' ? '正常运行' : '异常' }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="metric-card">
              <div slot="header" class="card-header">
                <span><i class="el-icon-cpu"></i> CPU 使用率</span>
              </div>
              <div class="metric-content">
                <div class="metric-value">{{ metrics.cpuUsage }}%</div>
                <el-progress :percentage="parseFloat(metrics.cpuUsage)" :status="getProgressStatus(parseFloat(metrics.cpuUsage))"></el-progress>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="metric-card">
              <div slot="header" class="card-header">
                <span><i class="el-icon-monitor"></i> JVM 内存使用</span>
              </div>
              <div class="metric-content">
                <div class="metric-value">{{ metrics.memoryUsed }}%</div>
                <el-progress :percentage="parseFloat(metrics.memoryUsed)" :status="getProgressStatus(parseFloat(metrics.memoryUsed))"></el-progress>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <el-card class="metric-card">
              <div slot="header" class="card-header">
                <span><i class="el-icon-connection"></i> 操作请求统计</span>
              </div>
              <div class="metric-content">
                <div class="metric-value">{{ metrics.httpRequests }}</div>
                <div class="metric-label">总记录数</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="metric-card">
              <div slot="header" class="card-header">
                <span><i class="el-icon-coin"></i> 数据库健康状态</span>
              </div>
              <div class="metric-content">
                <el-tag :type="metrics.dbStatus === 'UP' ? 'success' : 'danger'" size="large">
                  {{ metrics.dbStatus === 'UP' ? '连接正常' : '连接异常' }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="realtime-card" style="margin-top: 20px;">
          <div slot="header" class="card-header">
            <span><i class="el-icon-data-line"></i> 实时监控信息</span>
            <el-button type="primary" size="small" @click="refreshMetrics" :loading="loading">
              <i class="el-icon-refresh"></i> 刷新
            </el-button>
          </div>
          <div class="realtime-content">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="系统健康状态">
                <el-tag :type="metrics.health === 'UP' ? 'success' : 'danger'" size="small">
                  {{ metrics.health }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="CPU 使用率">{{ metrics.cpuUsage }}%</el-descriptions-item>
              <el-descriptions-item label="JVM 内存使用">{{ metrics.memoryUsed }}%</el-descriptions-item>
              <el-descriptions-item label="操作记录数">{{ metrics.httpRequests }}</el-descriptions-item>
              <el-descriptions-item label="数据库状态">
                <el-tag :type="metrics.dbStatus === 'UP' ? 'success' : 'danger'" size="small">
                  {{ metrics.dbStatus }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="最后更新时间">{{ lastUpdateTime }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="日志管理" name="logs">
        <el-card>
          <div slot="header" class="card-header">
            <span><i class="el-icon-notebook-2"></i> 操作日志</span>
            <div>
              <el-input
                v-model="logKeyword"
                placeholder="按模块/操作/操作人/详情搜索"
                clearable
                size="small"
                style="width: 280px; margin-right: 8px;"
                @keyup.enter.native="loadLogs"
              />
              <el-button size="small" type="primary" @click="loadLogs">查询</el-button>
              <el-button size="small" @click="resetLogs">重置</el-button>
            </div>
          </div>

          <el-table :data="logs" border stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="moduleName" label="模块" width="140" />
            <el-table-column prop="actionName" label="操作" width="180" />
            <el-table-column prop="operatorName" label="操作人" width="120" />
            <el-table-column prop="detail" label="详情" min-width="320" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="180" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="数据备份与恢复" name="backup">
        <el-card>
          <div slot="header" class="card-header">
            <span><i class="el-icon-folder-checked"></i> 数据备份与恢复</span>
            <div>
              <el-button size="small" type="primary" :loading="backupLoading" @click="createBackup">
                创建备份
              </el-button>
              <el-button size="small" @click="loadBackups">刷新列表</el-button>
            </div>
          </div>

          <el-alert
            title="恢复备份会覆盖当前业务数据，请确认后操作。"
            type="warning"
            show-icon
            :closable="false"
            style="margin-bottom: 12px;"
          />

          <el-table :data="backups" border stripe>
            <el-table-column prop="id" label="备份ID" width="90" />
            <el-table-column prop="backupName" label="备份名称" min-width="180" />
            <el-table-column prop="tableCount" label="表数" width="90" />
            <el-table-column prop="rowCount" label="数据行数" width="110" />
            <el-table-column prop="operatorName" label="操作人" width="120" />
            <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="140" fixed="right">
              <template slot-scope="scope">
                <el-button
                  type="danger"
                  size="mini"
                  :loading="restoreLoadingId === scope.row.id"
                  @click="restoreBackup(scope.row)"
                >
                  恢复
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import performanceApi from '@/api/performance'

export default {
  name: 'Performance',
  data() {
    return {
      activeTab: 'metrics',
      metrics: {
        health: 'UP',
        cpuUsage: '0',
        memoryUsed: '0',
        httpRequests: '0',
        dbStatus: 'UP'
      },
      logs: [],
      backups: [],
      logKeyword: '',
      loading: false,
      backupLoading: false,
      restoreLoadingId: null,
      lastUpdateTime: '',
      timer: null
    }
  },
  mounted() {
    if (!this.$store.getters.isLoggedIn) {
      return
    }
    this.loadAll()
    this.timer = setInterval(() => {
      if (this.activeTab === 'metrics') {
        this.fetchMetrics()
      }
    }, 5000)
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },
  methods: {
    async loadAll() {
      await Promise.all([this.fetchMetrics(), this.loadLogs(), this.loadBackups()])
    },
    async fetchMetrics() {
      try {
        const data = await performanceApi.getMetrics()
        this.metrics = data
        this.lastUpdateTime = this.formatTime(new Date())
      } catch (error) {
        this.$message.error('获取系统监控信息失败')
      }
    },
    refreshMetrics() {
      this.loading = true
      this.fetchMetrics().finally(() => {
        this.loading = false
      })
    },
    async loadLogs() {
      try {
        this.logs = await performanceApi.getLogs({
          keyword: this.logKeyword,
          limit: 300
        })
      } catch (e) {
        this.$message.error('加载日志失败')
      }
    },
    resetLogs() {
      this.logKeyword = ''
      this.loadLogs()
    },
    async loadBackups() {
      try {
        this.backups = await performanceApi.getBackups()
      } catch (e) {
        this.$message.error('加载备份列表失败')
      }
    },
    async createBackup() {
      this.backupLoading = true
      try {
        await performanceApi.createBackup({
          operator: this.$store.state.user ? this.$store.state.user.username : '',
          remark: '后台性能管理页面手动创建'
        })
        this.$message.success('创建备份成功')
        await Promise.all([this.loadBackups(), this.loadLogs(), this.fetchMetrics()])
      } catch (e) {
        this.$message.error(e.message || '创建备份失败')
      } finally {
        this.backupLoading = false
      }
    },
    restoreBackup(row) {
      this.$confirm(
        `确定恢复备份【${row.backupName}】吗？此操作会覆盖当前业务数据。`,
        '高风险操作确认',
        {
          confirmButtonText: '确认恢复',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        this.restoreLoadingId = row.id
        try {
          await performanceApi.restoreBackup(row.id, {
            operator: this.$store.state.user ? this.$store.state.user.username : ''
          })
          this.$message.success('恢复完成')
          await Promise.all([this.loadBackups(), this.loadLogs(), this.fetchMetrics()])
        } catch (e) {
          this.$message.error(e.message || '恢复备份失败')
        } finally {
          this.restoreLoadingId = null
        }
      }).catch(() => {})
    },
    getProgressStatus(percentage) {
      if (percentage >= 90) return 'exception'
      if (percentage >= 70) return 'warning'
      return 'success'
    },
    formatTime(date) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }
  }
}
</script>

<style scoped>
.performance-container {
  padding: 20px;
}

.metric-card {
  height: 200px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.card-header i {
  margin-right: 5px;
  color: #409EFF;
}

.metric-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100px;
}

.metric-value {
  font-size: 36px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 10px;
}

.metric-label {
  font-size: 14px;
  color: #909399;
}

.realtime-card {
  min-height: 300px;
}

.realtime-content {
  padding: 10px 0;
}
</style>
