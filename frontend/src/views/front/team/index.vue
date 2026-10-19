<template>
  <div class="front-team">
    <div class="team-header">
      <div class="header-content">
        <div class="logo">
          <i class="el-icon-football"></i>
          <span>足球比赛管理系统</span>
        </div>
        <div class="nav-menu">
          <el-menu :default-active="activeMenu" class="el-menu-demo" mode="horizontal" @select="handleSelect">
            <el-menu-item index="home">首页</el-menu-item>
            <el-menu-item index="match">赛事信息</el-menu-item>
            <el-menu-item index="team">球队展示</el-menu-item>
            <el-menu-item index="player">球员信息</el-menu-item>
            <el-menu-item index="ranking">排行榜</el-menu-item>
            <el-menu-item index="notice">公告</el-menu-item>
            <el-menu-item index="user">用户中心</el-menu-item>
            <el-menu-item index="team-management" v-if="user">球队管理</el-menu-item>
          </el-menu>
        </div>
        <div class="user-info">
          <span v-if="user">{{ user.nickname }} ({{ user.role === 'admin' ? '管理员' : user.role === 'referee' ? '裁判' : user.role === 'player' ? '运动员' : '普通用户' }})</span>
          <span v-else>游客</span>
          <el-button type="text" @click="handleLogout" v-if="user">退出</el-button>
          <el-button type="text" @click="showLoginDialog" v-else>登录/注册</el-button>
        </div>
      </div>
    </div>
    <div class="team-content">
      <div class="team-filter">
        <el-form :inline="true" :model="filterForm" class="demo-form-inline">
          <el-form-item label="联赛">
            <el-select v-model="filterForm.league" placeholder="请选择联赛">
              <el-option label="西甲" value="la liga"></el-option>
              <el-option label="英超" value="premier league"></el-option>
              <el-option label="德甲" value="bundesliga"></el-option>
              <el-option label="意甲" value="serie a"></el-option>
              <el-option label="法甲" value="ligue 1"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-input v-model="filterForm.keyword" placeholder="搜索球队名称" prefix-icon="el-icon-search"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="team-list">
        <el-card v-for="team in pagedTeams" :key="team.id" shadow="hover">
          <div class="team-card">
            <div class="team-logo">
              <i class="el-icon-football"></i>
            </div>
            <div class="team-info">
              <h3>{{ team.name }}</h3>
              <p class="team-league">{{ team.league }}</p>
              <p class="team-description">{{ team.description }}</p>
              <div class="team-stats">
                <span class="stat-item">
                  <i class="el-icon-s-finance"></i>
                  <span>排名: {{ team.rank }}</span>
                </span>
                <span class="stat-item">
                  <i class="el-icon-s-marketing"></i>
                  <span>积分: {{ team.points }}</span>
                </span>
                <span class="stat-item">
                  <i class="el-icon-time"></i>
                  <span>成立时间: {{ team.founded }}</span>
                </span>
              </div>
            </div>
            <el-button type="primary" @click="viewTeamDetails(team.id)">查看详情</el-button>
          </div>
        </el-card>
      </div>

      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[9, 18, 36]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="teams.length"
        />
      </div>
    </div>

    <!-- 登录/注册弹窗 -->
    <el-dialog :title="isRegister ? '用户注册' : '用户登录'" :visible.sync="dialogVisible" width="400px">
      <!-- 登录表单 -->
      <el-form
        v-if="!isRegister"
        ref="loginForm"
        :model="loginForm"
        :rules="loginRules"
        label-width="80px"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
            size="large"
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            size="large"
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 注册表单 -->
      <el-form
        v-else
        ref="registerForm"
        :model="registerForm"
        :rules="registerRules"
        label-width="80px"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            size="large"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="el-icon-lock"
            size="large"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="registerForm.nickname"
            placeholder="请输入昵称"
            prefix-icon="el-icon-user"
            size="large"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-actions">
        <el-button type="text" @click="toggleMode" v-if="!isRegister">
          还没有账号？立即注册
        </el-button>
        <el-button type="text" @click="toggleMode" v-else>
          已有账号？立即登录
        </el-button>
        <el-button type="text" @click="dialogVisible = false">
          取消
        </el-button>
      </div>

    </el-dialog>

    <!-- 球队详情弹窗 -->
    <el-dialog :title="currentTeam ? currentTeam.name + ' - 球队详情' : '球队详情'" :visible.sync="teamDetailDialog" width="900px">
      <div v-if="currentTeam" class="team-detail-content">
        <div class="team-basic-info">
          <div class="team-logo-large">
            <i class="el-icon-football"></i>
          </div>
          <div class="team-info">
            <h2>{{ currentTeam.name }}</h2>
            <div class="team-meta">
              <span class="meta-item"><i class="el-icon-map-location"></i> {{ currentTeam.city }}</span>
              <span class="meta-item"><i class="el-icon-user"></i> {{ currentTeam.coach }}</span>
              <span class="meta-item"><i class="el-icon-flag"></i> {{ currentTeam.status === 1 ? '活跃' : '非活跃' }}</span>
            </div>
            <div class="team-description">
              <h3>球队简介</h3>
              <p>{{ currentTeam.description || '暂无简介' }}</p>
            </div>
          </div>
        </div>

        <div class="team-members">
          <h3>球队成员</h3>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="球员" name="players">
              <el-table :data="teamPlayers" style="width: 100%">
                <el-table-column prop="id" label="球员ID" width="80" />
                <el-table-column prop="name" label="姓名" />
                <el-table-column prop="age" label="年龄" width="80" />
                <el-table-column prop="position" label="位置" width="100" />
                <el-table-column prop="jerseyNumber" label="球衣号码" width="100" />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="管理成员" name="members">
              <el-table :data="teamMembers" style="width: 100%">
                <el-table-column prop="userId" label="用户ID" width="80" />
                <el-table-column prop="userName" label="用户名" />
                <el-table-column prop="role" label="角色" width="100">
                  <template slot-scope="scope">
                    <el-tag :type="scope.row.role === 'captain' ? 'primary' : scope.row.role === 'admin' ? 'success' : 'info'">
                      {{ scope.row.role === 'captain' ? '队长' : scope.row.role === 'admin' ? '管理员' : '成员' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="teamDetailDialog = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as teamApi from '@/api/team'

export default {
  name: 'FrontTeam',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      activeMenu: 'team',
      dialogVisible: false,
      isRegister: false,
      loading: false,
      loginForm: {
        username: '',
        password: ''
      },
      registerForm: {
        username: '',
        password: '',
        confirmPassword: '',
        nickname: ''
      },
      loginRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
        ]
      },
      registerRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ],
        nickname: [
          { required: true, message: '请输入昵称', trigger: 'blur' }
        ]
      },
      filterForm: {
        league: '',
        keyword: ''
      },
      allTeams: [],
      teams: [],
      currentPage: 1,
      pageSize: 9,
      teamDetailDialog: false,
      currentTeam: null,
      activeTab: 'players',
      teamPlayers: [],
      teamMembers: []
    }
  },
  computed: {
    user() {
      return this.$store.state.user
    },
    pagedTeams() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.teams.slice(start, end)
    }
  },
  mounted() {
    this.loadTeamList()
  },
  methods: {
    handleSelect(key, keyPath) {
      this.activeMenu = key
      if (key === 'team-management') {
        this.$router.push('/front/team-management')
      } else {
        this.$router.push(`/front/${key}`)
      }
    },
    showLoginDialog() {
      this.dialogVisible = true
      this.isRegister = false
      this.loginForm = {
        username: '',
        password: ''
      }
      this.registerForm = {
        username: '',
        password: '',
        confirmPassword: '',
        nickname: ''
      }
      this.$nextTick(() => {
        if (this.$refs.loginForm) {
          this.$refs.loginForm.clearValidate()
        }
        if (this.$refs.registerForm) {
          this.$refs.registerForm.clearValidate()
        }
      })
    },
    toggleMode() {
      this.isRegister = !this.isRegister
      this.$nextTick(() => {
        if (this.isRegister) {
          this.$refs.registerForm.clearValidate()
        } else {
          this.$refs.loginForm.clearValidate()
        }
      })
    },
    handleLogin() {
      this.$refs.loginForm.validate(async (valid) => {
        if (valid) {
          this.loading = true
          try {
            await this.$store.dispatch('login', this.loginForm)
            this.$message.success('登录成功')
            this.dialogVisible = false
          } catch (error) {
            console.error('登录失败:', error)
            this.$message.error('登录失败：' + (error.message || '未知错误'))
          } finally {
            this.loading = false
          }
        }
      })
    },
    handleRegister() {
      this.$refs.registerForm.validate(async (valid) => {
        if (valid) {
          this.loading = true
          try {
            await this.$http.post('/api/user/register', {
              username: this.registerForm.username,
              password: this.registerForm.password,
              nickname: this.registerForm.nickname,
              role: 'user'
            })
            this.$message.success('注册成功，请登录')
            this.isRegister = false
            this.loginForm.username = this.registerForm.username
            this.loginForm.password = ''
            this.registerForm = {
              username: '',
              password: '',
              confirmPassword: '',
              nickname: ''
            }
          } catch (error) {
            console.error('注册失败:', error)
            this.$message.error('注册失败：' + (error.message || '未知错误'))
          } finally {
            this.loading = false
          }
        }
      })
    },
    handleLogout() {
      this.$confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await this.$store.dispatch('logout')
          this.$message.success('退出成功')
        } catch (error) {
          console.error('退出登录失败:', error)
          this.$message.error('退出登录失败')
        }
      })
    },
    loadTeamList() {
      teamApi.getAllTeams().then(res => {
        const rawTeams = Array.isArray(res) ? res : []
        this.allTeams = rawTeams.map(team => ({
          id: team.id,
          name: team.name,
          league: team.league || '足球联赛',
          description: team.description || '暂无描述',
          rank: Math.floor(Math.random() * 20) + 1,
          points: Math.floor(Math.random() * 100),
          founded: team.founded || '2025年',
          coach: team.coach,
          city: team.city
        }))
        this.applyFilter()
      }).catch(error => {
        console.error('获取球队列表失败:', error)
        this.$message.error('获取球队列表失败：' + (error.message || '未知错误'))
      })
    },
    applyFilter() {
      const keyword = (this.filterForm.keyword || '').trim().toLowerCase()
      const league = this.filterForm.league
      this.teams = this.allTeams.filter(team => {
        const matchedKeyword = !keyword || (team.name || '').toLowerCase().includes(keyword)
        const matchedLeague = !league || team.league === league
        return matchedKeyword && matchedLeague
      })
      this.currentPage = 1
    },
    handleSearch() {
      this.applyFilter()
    },
    resetForm() {
      this.filterForm = {
        league: '',
        keyword: ''
      }
      this.applyFilter()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
    },
    handleCurrentChange(page) {
      this.currentPage = page
    },
    viewTeamDetails(teamId) {
      // 获取球队详情
      teamApi.getTeamById(teamId).then(res => {
        this.currentTeam = res
        this.teamDetailDialog = true

        // 获取球队球员
        this.$http.get(`/api/player/team/${teamId}`).then(response => {
          this.teamPlayers = response.data || []
        }).catch(error => {
          console.error('获取球队球员失败:', error)
          this.teamPlayers = []
        })

        // 获取球队管理成员
        this.$http.get(`/api/team-member/team/${teamId}`).then(response => {
          this.teamMembers = response.data || []
        }).catch(error => {
          console.error('获取球队管理成员失败:', error)
          this.teamMembers = []
        })
      }).catch(error => {
        console.error('获取球队详情失败:', error)
        this.$message.error('获取球队详情失败：' + (error.message || '未知错误'))
      })
    }
  }
}
</script>

<style scoped>
.front-team {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.team-header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.logo i {
  font-size: 24px;
  margin-right: 8px;
  color: #409EFF;
}

.nav-menu {
  flex: 1;
  margin: 0 40px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.team-content {
  padding-top: 80px;
  padding-bottom: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding-left: 20px;
  padding-right: 20px;
}

.team-filter {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.team-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.team-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  text-align: center;
}

.team-logo {
  font-size: 64px;
  color: #409EFF;
  margin-bottom: 20px;
}

.team-info h3 {
  margin: 0 0 10px 0;
  font-size: 24px;
  color: #303133;
}

.team-league {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #606266;
}

.team-description {
  margin: 0 0 20px 0;
  font-size: 14px;
  color: #909399;
  line-height: 1.5;
}

.team-stats {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #606266;
}

.stat-item i {
  color: #409EFF;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.login-form {
  padding: 20px 0;
}

.login-btn {
  width: 100%;
}

.login-actions {
  text-align: center;
  margin-top: 15px;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.login-actions .el-button {
  color: #409EFF;
  padding: 0;
}

.login-actions .el-button:hover {
  color: #66b1ff;
}

.login-tips {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}

.login-tips p {
  margin: 4px 0;
}

.team-detail-content {
  padding: 20px 0;
}

.team-basic-info {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.team-logo-large {
  font-size: 80px;
  color: #409EFF;
  flex-shrink: 0;
}

.team-info {
  flex: 1;
}

.team-info h2 {
  margin: 0 0 15px 0;
  font-size: 28px;
  color: #303133;
}

.team-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #606266;
  font-size: 16px;
}

.meta-item i {
  color: #409EFF;
}

.team-description {
  margin-top: 20px;
}

.team-description h3 {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #303133;
}

.team-description p {
  margin: 0;
  color: #606266;
  line-height: 1.5;
}

.team-members {
  margin-top: 30px;
}

.team-members h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #303133;
}

.dialog-footer {
  text-align: center;
}
</style>
