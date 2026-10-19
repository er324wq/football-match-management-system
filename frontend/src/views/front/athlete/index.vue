<template>
  <div class="front-athlete">
    <div class="athlete-header">
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
    <div class="athlete-content">
      <div v-if="!user" class="login-prompt">
        <el-card shadow="hover">
          <div class="prompt-content">
            <i class="el-icon-user" style="font-size: 64px; color: #409EFF; margin-bottom: 20px;"></i>
            <h2>请先登录</h2>
            <p>登录后可以查看运动员中心</p>
            <el-button type="primary" size="large" @click="showLoginDialog">立即登录</el-button>
          </div>
        </el-card>
      </div>
      <div v-else-if="user.role !== 'player' && user.role !== 'admin'" class="role-prompt">
        <el-card shadow="hover">
          <div class="prompt-content">
            <i class="el-icon-warning" style="font-size: 64px; color: #E6A23C; margin-bottom: 20px;"></i>
            <h2>权限不足</h2>
            <p>您不是运动员，无法访问运动员中心</p>
            <el-button type="primary" @click="applyForAthlete">申请成为运动员</el-button>
          </div>
        </el-card>
      </div>
      <div v-else class="athlete-dashboard">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card shadow="hover">
              <div class="athlete-profile">
                <div class="avatar">
                  <i class="el-icon-user"></i>
                </div>
                <h3>{{ athleteInfo.name || user.nickname }}</h3>
                <p class="athlete-number">编号: {{ athleteInfo.number || 'N/A' }}</p>
                <p class="athlete-position">位置: {{ getPositionText(athleteInfo.position) }}</p>
                <el-button type="primary" @click="editProfile">编辑资料</el-button>
              </div>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card shadow="hover">
              <div class="athlete-stats">
                <h3>个人数据</h3>
                <el-row :gutter="20">
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ athleteStats.goals || 0 }}</div>
                      <div class="stat-label">进球</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ athleteStats.assists || 0 }}</div>
                      <div class="stat-label">助攻</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ athleteStats.matches || 0 }}</div>
                      <div class="stat-label">出场</div>
                    </div>
                  </el-col>
                </el-row>
                <el-row :gutter="20" style="margin-top: 20px;">
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ athleteStats.yellowCards || 0 }}</div>
                      <div class="stat-label">黄牌</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ athleteStats.redCards || 0 }}</div>
                      <div class="stat-label">红牌</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ athleteStats.goalRatio || 0 }}%</div>
                      <div class="stat-label">进球率</div>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="24">
            <el-card shadow="hover">
              <div class="athlete-matches">
                <h3>最近比赛</h3>
                <el-table :data="recentMatches" style="width: 100%">
                  <el-table-column prop="date" label="日期" width="120"></el-table-column>
                  <el-table-column prop="opponent" label="对手" width="150"></el-table-column>
                  <el-table-column prop="result" label="结果" width="120">
                    <template slot-scope="scope">
                      <span :class="{ 'win': scope.row.result === 'win', 'loss': scope.row.result === 'loss', 'draw': scope.row.result === 'draw' }">
                        {{ getResultText(scope.row.result) }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="goals" label="进球" width="80"></el-table-column>
                  <el-table-column prop="assists" label="助攻" width="80"></el-table-column>
                  <el-table-column prop="minutes" label="出场时间" width="100"></el-table-column>
                  <el-table-column label="详情" width="80">
                    <template slot-scope="scope">
                      <el-button type="text" @click="viewMatchDetails(scope.row.id)">查看</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <el-card shadow="hover">
              <div class="athlete-trainings">
                <h3>训练计划</h3>
                <el-timeline>
                  <el-timeline-item
                    v-for="(training, index) in trainingSchedule"
                    :key="index"
                    :timestamp="training.date"
                    type="primary"
                  >
                    <div class="training-item">
                      <h4>{{ training.title }}</h4>
                      <p>{{ training.description }}</p>
                      <span class="training-time">{{ training.time }}</span>
                    </div>
                  </el-timeline-item>
                </el-timeline>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <div class="athlete-messages">
                <h3>系统消息</h3>
                <el-list>
                  <el-list-item v-for="(message, index) in systemMessages" :key="index">
                    <div class="message-item">
                      <h4>{{ message.title }}</h4>
                      <p class="message-content">{{ message.content }}</p>
                      <span class="message-time">{{ message.time }}</span>
                    </div>
                  </el-list-item>
                </el-list>
              </div>
            </el-card>
          </el-col>
        </el-row>
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
export default {
  name: 'FrontAthlete',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      activeMenu: 'athlete',
      user: null,
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
      athleteInfo: {
        name: 'Cristiano Ronaldo',
        number: 7,
        position: 'forward',
        team: '皇家马德里',
        age: 37,
        height: '1.87m',
        weight: '83kg'
      },
      athleteStats: {
        goals: 700,
        assists: 200,
        matches: 900,
        yellowCards: 100,
        redCards: 10,
        goalRatio: 30
      },
      recentMatches: [
        {
          id: 1,
          date: '2026-04-01',
          opponent: '巴塞罗那',
          result: 'win',
          goals: 2,
          assists: 1,
          minutes: 90
        },
        {
          id: 2,
          date: '2026-03-25',
          opponent: '利物浦',
          result: 'draw',
          goals: 1,
          assists: 0,
          minutes: 90
        },
        {
          id: 3,
          date: '2026-03-18',
          opponent: '拜仁慕尼黑',
          result: 'win',
          goals: 3,
          assists: 1,
          minutes: 90
        }
      ],
      trainingSchedule: [
        {
          title: '体能训练',
          description: '有氧训练和力量训练',
          date: '2026-04-03',
          time: '14:00-16:00'
        },
        {
          title: '战术训练',
          description: '球队战术演练',
          date: '2026-04-04',
          time: '15:00-17:00'
        },
        {
          title: '技术训练',
          description: '射门和传球练习',
          date: '2026-04-05',
          time: '16:00-18:00'
        }
      ],
      systemMessages: [
        {
          title: '比赛通知',
          content: '您已被列入下一场比赛的首发阵容',
          time: '2026-04-01 10:30'
        },
        {
          title: '训练提醒',
          content: '明天下午有体能训练，请准时参加',
          time: '2026-04-02 14:00'
        },
        {
          title: '系统更新',
          content: '运动员中心功能已更新，请查看新功能',
          time: '2026-04-01 09:00'
        }
      ]
    }
  },
  mounted() {
    this.user = this.$store.state.user
  },
  methods: {
    handleSelect(key, keyPath) {
      this.activeMenu = key
      this.$router.push(`/front/${key}`)
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
            const response = await this.$store.dispatch('login', this.loginForm)
            this.$message.success('登录成功')
            this.dialogVisible = false
            this.user = response.user
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
          this.user = null
        } catch (error) {
          console.error('退出登录失败:', error)
          this.$message.error('退出登录失败')
        }
      })
    },
    applyForAthlete() {
      this.$router.push('/front/apply')
    },
    getPositionText(position) {
      const positionMap = {
        forward: '前锋',
        midfielder: '中场',
        defender: '后卫',
        goalkeeper: '守门员'
      }
      return positionMap[position] || '未知'
    },
    getResultText(result) {
      const resultMap = {
        win: '胜利',
        loss: '失败',
        draw: '平局'
      }
      return resultMap[result] || '未知'
    },
    editProfile() {
      // 实际项目中这里会打开编辑资料弹窗
      this.$message.info('编辑资料功能正在开发中')
    },
    viewMatchDetails(matchId) {
      // 实际项目中这里会跳转到比赛详情页面
      this.$message.info(`查看比赛ID: ${matchId} 的详情`)
    }
  }
}
</script>

<style scoped>
.front-athlete {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.athlete-header {
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

.athlete-content {
  padding-top: 80px;
  padding-bottom: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding-left: 20px;
  padding-right: 20px;
}

.login-prompt,
.role-prompt {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 500px;
}

.prompt-content {
  text-align: center;
  padding: 40px;
}

.athlete-profile {
  text-align: center;
  padding: 20px;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background-color: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.avatar i {
  font-size: 48px;
  color: #409EFF;
}

.athlete-profile h3 {
  margin: 0 0 10px 0;
  font-size: 20px;
  color: #303133;
}

.athlete-number,
.athlete-position {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #606266;
}

.athlete-stats h3,
.athlete-matches h3,
.athlete-trainings h3,
.athlete-messages h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #303133;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background-color: #f9fafc;
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.win {
  color: #67C23A;
  font-weight: bold;
}

.loss {
  color: #F56C6C;
  font-weight: bold;
}

.draw {
  color: #E6A23C;
  font-weight: bold;
}

.training-item {
  padding: 10px;
  background-color: #f9fafc;
  border-radius: 8px;
}

.training-item h4 {
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #303133;
}

.training-item p {
  margin: 0 0 10px 0;
  font-size: 12px;
  color: #606266;
}

.training-time {
  font-size: 12px;
  color: #909399;
}

.message-item {
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
}

.message-item h4 {
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #303133;
}

.message-content {
  margin: 0 0 10px 0;
  font-size: 12px;
  color: #606266;
}

.message-time {
  font-size: 12px;
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
