<template>
  <div class="dashboard">
    <el-card class="welcome-card">
      <template slot="header">
        <div class="card-header">
          <span>欢迎使用足球比赛管理系统</span>
        </div>
      </template>
      <div class="welcome-content">
        <p>您好，{{ user ? user.nickname : '管理员' }}！</p>
        <p>这是一个功能完善的足球比赛管理系统，您可以通过左侧菜单访问各个功能模块。</p>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <i class="el-icon-user"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.userCount }}</div>
              <div class="stat-label">用户数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <i class="el-icon-football"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.teamCount }}</div>
              <div class="stat-label">球队数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <i class="el-icon-s-custom"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.playerCount }}</div>
              <div class="stat-label">球员数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <i class="el-icon-date"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.matchCount }}</div>
              <div class="stat-label">比赛数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as userApi from '@/api/user'
import * as teamApi from '@/api/team'
import * as playerApi from '@/api/player'
import * as matchApi from '@/api/match'

export default {
  name: 'Dashboard',
  data() {
    return {
      stats: {
        userCount: 0,
        teamCount: 0,
        playerCount: 0,
        matchCount: 0
      }
    }
  },
  computed: {
    user() {
      return this.$store.state.user
    }
  },
  mounted() {
    this.loadStats()
  },
  methods: {
    async loadStats() {
      try {
        const [userRes, teamRes, playerRes, matchRes] = await Promise.all([
          userApi.getUserList({ page: 1, size: 1 }),
          teamApi.getTeamList({ page: 1, size: 1 }),
          playerApi.getPlayerList({ page: 1, size: 1 }),
          matchApi.getMatchList({ page: 1, size: 1 })
        ])

        this.stats = {
          userCount: Number(userRes.total || 0),
          teamCount: Number(teamRes.total || 0),
          playerCount: Number(playerRes.total || 0),
          matchCount: Number(matchRes.total || 0)
        }
      } catch (error) {
        console.error('加载统计数据失败:', error)
        this.$message.error('加载统计数据失败')
      }
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.welcome-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-content {
  padding: 20px 0;
}

.welcome-content p {
  margin: 8px 0;
  color: #606266;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #f0f9eb;
  color: #67c23a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}
</style>
