<template>
  <div class="front-player">
    <div class="player-header">
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
    <div class="player-content">
      <div class="player-filter">
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
          <el-form-item label="球队">
            <el-select v-model="filterForm.team" placeholder="请选择球队">
              <el-option label="皇家马德里" value="real madrid"></el-option>
              <el-option label="巴塞罗那" value="barcelona"></el-option>
              <el-option label="利物浦" value="liverpool"></el-option>
              <el-option label="曼城" value="man city"></el-option>
              <el-option label="拜仁慕尼黑" value="bayern munich"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="位置">
            <el-select v-model="filterForm.position" placeholder="请选择位置">
              <el-option label="前锋" value="forward"></el-option>
              <el-option label="中场" value="midfielder"></el-option>
              <el-option label="后卫" value="defender"></el-option>
              <el-option label="守门员" value="goalkeeper"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-input v-model="filterForm.keyword" placeholder="搜索球员名称" prefix-icon="el-icon-search"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="player-list">
        <el-card v-for="player in players" :key="player.id" shadow="hover">
          <div class="player-card">
            <div class="player-avatar">
              <i class="el-icon-user"></i>
            </div>
            <div class="player-info">
              <h3>{{ player.name }}</h3>
              <div class="player-basic-info">
                <span class="info-item">
                  <i class="el-icon-s-home"></i>
                  <span>{{ player.team }}</span>
                </span>
                <span class="info-item">
                  <i class="el-icon-s-flag"></i>
                  <span>{{ player.position }}</span>
                </span>
                <span class="info-item">
                  <i class="el-icon-s-unfold"></i>
                  <span>{{ player.number }}</span>
                </span>
              </div>
              <div class="player-stats">
                <span class="stat-item">
                  <i class="el-icon-s-marketing"></i>
                  <span>进球: {{ player.goals }}</span>
                </span>
                <span class="stat-item">
                  <i class="el-icon-s-finance"></i>
                  <span>助攻: {{ player.assists }}</span>
                </span>
                <span class="stat-item">
                  <i class="el-icon-time"></i>
                  <span>出场: {{ player.appearances }}</span>
                </span>
              </div>
            </div>
            <el-button type="primary" @click="viewPlayerDetails(player.id)">查看详情</el-button>
          </div>
        </el-card>
      </div>

      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
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
  </div>
</template>

<script>
import * as playerApi from '@/api/player'
import * as teamApi from '@/api/team'

export default {
  name: 'FrontPlayer',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      activeMenu: 'player',
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
        team: '',
        position: '',
        keyword: ''
      },
      currentPage: 1,
      pageSize: 10,
      total: 0,
      players: [],
      teams: []
    }
  },
  computed: {
    user() {
      return this.$store.state.user
    }
  },
  mounted() {
    this.loadTeams()
    this.loadPlayerList()
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
    loadTeams() {
      teamApi.getAllTeams().then(res => {
        this.teams = res
      }).catch(error => {
        console.error('获取球队列表失败:', error)
        this.$message.error('获取球队列表失败：' + (error.message || '未知错误'))
      })
    },
    loadPlayerList() {
      playerApi.getPlayersWithTeam().then(res => {
        // 过滤数据
        let filteredList = res
        if (this.filterForm.keyword) {
          filteredList = filteredList.filter(player => player.name.includes(this.filterForm.keyword))
        }
        if (this.filterForm.position) {
          filteredList = filteredList.filter(player => player.position === this.filterForm.position)
        }
        // 分页处理
        this.total = filteredList.length
        const start = (this.currentPage - 1) * this.pageSize
        const end = start + this.pageSize
        // 转换数据格式以适应前端显示
        this.players = filteredList.slice(start, end).map(player => {
          return {
            id: player.id,
            name: player.name,
            team: player.teamName || '暂无球队',
            position: player.position,
            number: player.jerseyNumber,
            goals: Math.floor(Math.random() * 30), // 模拟进球数
            assists: Math.floor(Math.random() * 20), // 模拟助攻数
            appearances: Math.floor(Math.random() * 30) + 1 // 模拟出场次数
          }
        })
      }).catch(error => {
        console.error('获取球员列表失败:', error)
        this.$message.error('获取球员列表失败：' + (error.message || '未知错误'))
      })
    },
    handleSearch() {
      this.currentPage = 1
      this.loadPlayerList()
    },
    resetForm() {
      this.filterForm = {
        league: '',
        team: '',
        position: '',
        keyword: ''
      }
      this.currentPage = 1
      this.loadPlayerList()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.loadPlayerList()
    },
    handleCurrentChange(current) {
      this.currentPage = current
      this.loadPlayerList()
    },
    viewPlayerDetails(playerId) {
      // 实际项目中这里会跳转到球员详情页面
      // 现在我们可以通过API获取球员详情并显示
      playerApi.getPlayerById(playerId).then(res => {
        const player = res
        this.$message.info(`球员详情：\n姓名：${player.name}\n年龄：${player.age}\n位置：${player.position}\n球衣号码：${player.jerseyNumber}\n所属球队：${player.teamName || '暂无球队'}`)
      }).catch(error => {
        console.error('获取球员详情失败:', error)
        this.$message.error('获取球员详情失败：' + (error.message || '未知错误'))
      })
    }
  }
}
</script>

<style scoped>
.front-player {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.player-header {
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

.player-content {
  padding-top: 80px;
  padding-bottom: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding-left: 20px;
  padding-right: 20px;
}

.player-filter {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.player-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.player-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  text-align: center;
}

.player-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background-color: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.player-avatar i {
  font-size: 48px;
  color: #409EFF;
}

.player-info h3 {
  margin: 0 0 10px 0;
  font-size: 20px;
  color: #303133;
}

.player-basic-info {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #606266;
}

.info-item i {
  color: #409EFF;
}

.player-stats {
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
</style>
