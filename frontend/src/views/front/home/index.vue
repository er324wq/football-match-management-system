<template>
  <div class="front-home">
    <div class="home-header">
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
          <div v-else>
            <el-button type="text" @click="$router.push('/login')">后台登录</el-button>
            <el-button type="text" @click="showLoginDialog">登录</el-button>
            <el-button type="text" @click="showRegisterDialog">注册</el-button>
          </div>
        </div>
      </div>
    </div>
    <div class="home-content">


      <el-carousel :interval="5000" type="card" height="400px">
        <el-carousel-item v-for="item in carouselItems" :key="item.id">
          <div class="carousel-item">
            <img :src="item.image" :alt="item.title">
            <div class="carousel-caption">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>

      <div class="section-container">
        <div class="section">
          <h2>最新赛事</h2>
          <el-row :gutter="20">
            <el-col :span="8" v-for="match in latestMatches" :key="match.id">
              <el-card shadow="hover">
                <div class="match-card">
                  <div class="match-info">
                    <div class="team-name">{{ match.homeTeam }}</div>
                    <div class="match-score">{{ match.homeScore }} - {{ match.awayScore }}</div>
                    <div class="team-name">{{ match.awayTeam }}</div>
                  </div>
                  <div class="match-time">{{ match.time }}</div>
                  <div class="match-venue">{{ match.venue }}</div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <div class="section">
          <h2>热门球队</h2>
          <el-row :gutter="20">
            <el-col :span="6" v-for="team in hotTeams" :key="team.id">
              <el-card shadow="hover">
                <div class="team-card">
                  <div class="team-logo">
                    <i class="el-icon-football"></i>
                  </div>
                  <div class="team-info">
                    <h3>{{ team.name }}</h3>
                    <p>{{ team.description }}</p>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <div class="section">
          <h2>最新公告</h2>
          <el-card>
            <div class="notice-list">
              <div class="notice-item" v-for="notice in latestNotices" :key="notice.id">
                <span class="notice-title">{{ notice.title }}</span>
                <span class="notice-time">{{ notice.time }}</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog title="用户注册" :visible.sync="dialogVisible" width="400px">
      <el-form
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
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号"
            prefix-icon="el-icon-mobile-phone"
            size="large"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱"
            prefix-icon="el-icon-message"
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
        <el-button type="text" @click="dialogVisible = false">
          取消
        </el-button>
      </div>
    </el-dialog>

    <!-- 登录弹窗 -->
    <el-dialog title="用户登录" :visible.sync="loginDialogVisible" width="400px">
      <el-form
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
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            size="large"
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

      <div class="login-actions">
        <el-button type="text" @click="loginDialogVisible = false">
          取消
        </el-button>
      </div>
    </el-dialog>


  </div>
</template>

<script>
import * as matchApi from '@/api/match'
import * as teamApi from '@/api/team'

export default {
  name: 'FrontHome',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      activeMenu: 'home',
      dialogVisible: false,
      loginDialogVisible: false,
      loading: false,
      registerForm: {
        username: '',
        password: '',
        confirmPassword: '',
        nickname: '',
        phone: '',
        email: ''
      },
      loginForm: {
        username: '',
        password: ''
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
        ],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1\d{10}$/, message: '请输入有效的11位手机号', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入有效邮箱', trigger: 'blur' }
        ]
      },
      loginRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      },
      carouselItems: [
        {
          id: 1,
          title: '2026赛季足球联赛即将开始',
          description: '欢迎各位球迷朋友关注本赛季的精彩赛事',
          image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=football%20match%20stadium%20with%20crowd&image_size=landscape_16_9'
        },
        {
          id: 2,
          title: '新赛季球队阵容大揭秘',
          description: '各大球队新赛季阵容已经确定，让我们拭目以待',
          image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=football%20team%20lineup%20on%20field&image_size=landscape_16_9'
        },
        {
          id: 3,
          title: '球迷嘉年华活动',
          description: '参与线上线下活动，赢取精美礼品',
          image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=football%20fans%20celebration&image_size=landscape_16_9'
        }
      ],
      latestMatches: [],
      hotTeams: [],
      latestNotices: [
        {
          id: 1,
          title: '关于2026赛季赛程安排的通知',
          time: '2026-03-30'
        },
        {
          id: 2,
          title: '球迷嘉年华活动报名开始',
          time: '2026-03-28'
        },
        {
          id: 3,
          title: '新赛季球员转会信息',
          time: '2026-03-25'
        }
      ]
    }
  },
  computed: {
    user() {
      return this.$store.state.user
    }
  },
  mounted() {
    this.loadLatestMatches()
    this.loadHotTeams()
  },
  methods: {
    handleSelect(key, keyPath) {
      this.activeMenu = key
      this.$router.push(`/front/${key}`)
    },

    showRegisterDialog() {
      this.dialogVisible = true
      this.registerForm = {
        username: '',
        password: '',
        confirmPassword: '',
        nickname: '',
        phone: '',
        email: ''
      }
      this.$nextTick(() => {
        if (this.$refs.registerForm) {
          this.$refs.registerForm.clearValidate()
        }
      })
    },
    showLoginDialog() {
      this.loginDialogVisible = true
      this.loginForm = {
        username: '',
        password: ''
      }
      this.$nextTick(() => {
        if (this.$refs.loginForm) {
          this.$refs.loginForm.clearValidate()
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
              phone: this.registerForm.phone,
              email: this.registerForm.email,
              role: 'user'
            })
            this.$message.success('注册成功，请登录')
            this.dialogVisible = false
            this.registerForm = {
              username: '',
              password: '',
              confirmPassword: '',
              nickname: '',
              phone: '',
              email: ''
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
    handleLogin() {
      this.$refs.loginForm.validate(async (valid) => {
        if (valid) {
          this.loading = true
          try {
            const response = await this.$store.dispatch('login', this.loginForm)
            this.$message.success('登录成功')
            this.loginDialogVisible = false
          } catch (error) {
            console.error('登录失败:', error)
            this.$message.error('登录失败：' + (error.message || '未知错误'))
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
    loadLatestMatches() {
      matchApi.getMatchList({ page: 1, size: 3, status: '' }).then(res => {
        this.latestMatches = res.records.map(match => {
          return {
            id: match.id,
            homeTeam: match.homeTeamName,
            awayTeam: match.awayTeamName,
            homeScore: match.homeScore || 0,
            awayScore: match.awayScore || 0,
            time: match.matchTime,
            venue: match.stadium
          }
        })
      }).catch(error => {
        console.error('获取最新赛事失败:', error)
      })
    },
    loadHotTeams() {
      teamApi.getAllTeams().then(res => {
        // 取前4个球队作为热门球队
        this.hotTeams = res.slice(0, 4).map(team => {
          return {
            id: team.id,
            name: team.name,
            description: team.description || '暂无描述'
          }
        })
      }).catch(error => {
        console.error('获取热门球队失败:', error)
      })
    }
  }
}
</script>

<style scoped>
.front-home {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.home-header {
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

.home-content {
  padding-top: 80px;
  padding-bottom: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding-left: 20px;
  padding-right: 20px;
}

.carousel-item {
  position: relative;
  height: 400px;
  overflow: hidden;
}

.carousel-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-caption {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 20px;
}

.carousel-caption h3 {
  margin: 0 0 10px 0;
  font-size: 24px;
}

.carousel-caption p {
  margin: 0;
  font-size: 16px;
}

.section-container {
  margin-top: 40px;
}

.section {
  margin-bottom: 40px;
}

.section h2 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409EFF;
}

.match-card {
  text-align: center;
  padding: 20px;
}

.match-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
}

.team-name {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.match-score {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.match-time, .match-venue {
  font-size: 14px;
  color: #606266;
  margin-top: 5px;
}

.team-card {
  text-align: center;
  padding: 20px;
}

.team-logo {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 15px;
}

.team-info h3 {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #303133;
}

.team-info p {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.notice-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #ebeef5;
}

.notice-title {
  font-size: 16px;
  color: #303133;
  cursor: pointer;
}

.notice-title:hover {
  color: #409EFF;
}

.notice-time {
  font-size: 14px;
  color: #909399;
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
