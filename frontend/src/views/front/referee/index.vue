<template>
  <div class="front-referee">
    <div class="referee-header">
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
    <div class="referee-content">
      <div v-if="!user" class="login-prompt">
        <el-card shadow="hover">
          <div class="prompt-content">
            <i class="el-icon-user" style="font-size: 64px; color: #409EFF; margin-bottom: 20px;"></i>
            <h2>请先登录</h2>
            <p>登录后可以查看裁判中心</p>
            <el-button type="primary" size="large" @click="showLoginDialog">立即登录</el-button>
          </div>
        </el-card>
      </div>
      <div v-else-if="user.role !== 'referee' && user.role !== 'admin'" class="role-prompt">
        <el-card shadow="hover">
          <div class="prompt-content">
            <i class="el-icon-warning" style="font-size: 64px; color: #E6A23C; margin-bottom: 20px;"></i>
            <h2>权限不足</h2>
            <p>您不是裁判，无法访问裁判中心</p>
            <el-button type="primary" @click="applyForReferee">申请成为裁判</el-button>
          </div>
        </el-card>
      </div>
      <div v-else class="referee-dashboard">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card shadow="hover">
              <div class="referee-profile">
                <div class="avatar">
                  <i class="el-icon-s-flag"></i>
                </div>
                <h3>{{ refereeInfo.name || user.nickname }}</h3>
                <p class="referee-level">等级: {{ getLevelText(refereeInfo.level) }}</p>
                <p class="referee-experience">执裁场次: {{ refereeInfo.experience || 0 }}</p>
                <el-button type="primary" @click="editProfile">编辑资料</el-button>
              </div>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card shadow="hover">
              <div class="referee-stats">
                <h3>执裁数据</h3>
                <el-row :gutter="20">
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ refereeStats.matches || 0 }}</div>
                      <div class="stat-label">执裁场次</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ refereeStats.yellowCards || 0 }}</div>
                      <div class="stat-label">黄牌</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ refereeStats.redCards || 0 }}</div>
                      <div class="stat-label">红牌</div>
                    </div>
                  </el-col>
                </el-row>
                <el-row :gutter="20" style="margin-top: 20px;">
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ refereeStats.penalties || 0 }}</div>
                      <div class="stat-label">点球</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ refereeStats.averageRating || 0 }}</div>
                      <div class="stat-label">平均评分</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="stat-item">
                      <div class="stat-value">{{ refereeStats.correctDecisions || 0 }}%</div>
                      <div class="stat-label">正确判罚率</div>
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
              <div class="referee-schedule">
                <h3>执裁安排</h3>
                <el-table :data="refereeSchedule" style="width: 100%">
                  <el-table-column prop="date" label="日期" width="120"></el-table-column>
                  <el-table-column prop="time" label="时间" width="100"></el-table-column>
                  <el-table-column prop="match" label="比赛" width="200"></el-table-column>
                  <el-table-column prop="venue" label="场地" width="150"></el-table-column>
                  <el-table-column prop="status" label="状态" width="100">
                    <template slot-scope="scope">
                      <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="120">
                    <template slot-scope="scope">
                      <el-button type="text" @click="viewMatchDetails(scope.row.id)">查看详情</el-button>
                      <el-button type="text" @click="confirmMatch(scope.row.id)" v-if="scope.row.status === 'pending'">确认</el-button>
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
              <div class="referee-assessments">
                <h3>执裁评价</h3>
                <el-list>
                  <el-list-item v-for="(assessment, index) in refereeAssessments" :key="index">
                    <div class="assessment-item">
                      <div class="assessment-header">
                        <h4>{{ assessment.match }}</h4>
                        <span class="assessment-date">{{ assessment.date }}</span>
                      </div>
                      <div class="assessment-rating">
                        <span class="rating-label">评分:</span>
                        <el-rate v-model="assessment.rating" disabled show-score></el-rate>
                      </div>
                      <p class="assessment-comment">{{ assessment.comment }}</p>
                    </div>
                  </el-list-item>
                </el-list>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <div class="referee-messages">
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
  name: 'FrontReferee',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      activeMenu: 'referee',
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
      refereeInfo: {
        name: 'Pierluigi Collina',
        level: 'international',
        experience: 1000,
        age: 60,
        license: 'FIFA国际裁判',
        languages: ['意大利语', '英语', '西班牙语']
      },
      refereeStats: {
        matches: 1000,
        yellowCards: 5000,
        redCards: 500,
        penalties: 1000,
        averageRating: 4.8,
        correctDecisions: 95
      },
      refereeSchedule: [
        {
          id: 1,
          date: '2026-04-05',
          time: '20:00',
          match: '皇家马德里 vs 巴塞罗那',
          venue: '伯纳乌球场',
          status: 'pending'
        },
        {
          id: 2,
          date: '2026-04-10',
          time: '19:30',
          match: '利物浦 vs 曼城',
          venue: '安菲尔德球场',
          status: 'confirmed'
        },
        {
          id: 3,
          date: '2026-04-15',
          time: '21:00',
          match: '拜仁慕尼黑 vs 多特蒙德',
          venue: '安联球场',
          status: 'confirmed'
        }
      ],
      refereeAssessments: [
        {
          match: '皇家马德里 vs 马德里竞技',
          date: '2026-03-30',
          rating: 4.5,
          comment: '判罚准确，执法公正，控制比赛节奏良好'
        },
        {
          match: '巴塞罗那 vs 塞维利亚',
          date: '2026-03-25',
          rating: 4.8,
          comment: '表现出色，关键时刻判罚果断，得到双方认可'
        },
        {
          match: '曼联 vs 阿森纳',
          date: '2026-03-20',
          rating: 4.2,
          comment: '整体表现不错，个别判罚有待商榷'
        }
      ],
      systemMessages: [
        {
          title: '执裁通知',
          content: '您已被指派执裁4月5日皇家马德里 vs 巴塞罗那的比赛',
          time: '2026-04-01 10:30'
        },
        {
          title: '培训提醒',
          content: '4月3日有裁判培训课程，请准时参加',
          time: '2026-04-02 14:00'
        },
        {
          title: '系统更新',
          content: '裁判中心功能已更新，请查看新功能',
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
    applyForReferee() {
      this.$router.push('/front/apply')
    },
    getLevelText(level) {
      const levelMap = {
        international: '国际级',
        national: '国家级',
        provincial: '省级'
      }
      return levelMap[level] || '未知'
    },
    getStatusType(status) {
      const statusMap = {
        pending: 'warning',
        confirmed: 'success',
        completed: 'info'
      }
      return statusMap[status] || 'info'
    },
    getStatusText(status) {
      const statusMap = {
        pending: '待确认',
        confirmed: '已确认',
        completed: '已完成'
      }
      return statusMap[status] || ''
    },
    editProfile() {
      // 实际项目中这里会打开编辑资料弹窗
      this.$message.info('编辑资料功能正在开发中')
    },
    viewMatchDetails(matchId) {
      // 实际项目中这里会跳转到比赛详情页面
      this.$message.info(`查看比赛ID: ${matchId} 的详情`)
    },
    confirmMatch(matchId) {
      // 实际项目中这里会调用API确认执裁
      this.$message.success('确认执裁成功')
      const match = this.refereeSchedule.find(item => item.id === matchId)
      if (match) {
        match.status = 'confirmed'
      }
    }
  }
}
</script>

<style scoped>
.front-referee {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.referee-header {
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

.referee-content {
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

.referee-profile {
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

.referee-profile h3 {
  margin: 0 0 10px 0;
  font-size: 20px;
  color: #303133;
}

.referee-level,
.referee-experience {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #606266;
}

.referee-stats h3,
.referee-schedule h3,
.referee-assessments h3,
.referee-messages h3 {
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

.assessment-item {
  padding: 15px;
  background-color: #f9fafc;
  border-radius: 8px;
  margin-bottom: 15px;
}

.assessment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.assessment-header h4 {
  margin: 0;
  font-size: 14px;
  color: #303133;
}

.assessment-date {
  font-size: 12px;
  color: #909399;
}

.assessment-rating {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}

.rating-label {
  margin-right: 10px;
  font-size: 14px;
  color: #606266;
}

.assessment-comment {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
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
