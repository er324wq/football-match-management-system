<template>
  <div class="frontend-container">
    <div class="frontend-header">
      <div class="header-content">
        <div class="logo">
          <i class="el-icon-football"></i>
          <span>足球比赛管理系统</span>
        </div>
        <div class="user-info">
          <span v-if="user">{{ user.nickname }} ({{ user.role === 'admin' ? '管理员' : user.role === 'referee' ? '裁判' : user.role === 'player' ? '运动员' : '普通用户' }})</span>
          <span v-else>游客</span>
          <el-button type="text" @click="handleLogout" v-if="user">退出</el-button>
          <el-button type="text" @click="showLoginDialog" v-else>登录/注册</el-button>
        </div>
      </div>
    </div>
    <div class="frontend-content">
      <el-card class="welcome-card">
        <div class="welcome-content">
          <i class="el-icon-football" style="font-size: 64px; color: #409EFF;"></i>
          <h2>欢迎使用足球比赛管理系统前台界面</h2>
          <p v-if="!user">您当前处于游客模式，功能受限。请登录以获得完整功能。</p>
          <p v-else-if="user.role === 'user'">您已登录为普通用户，可以使用前台功能。</p>
          <p v-else>您已登录为{{ user.role === 'admin' ? '管理员' : user.role === 'referee' ? '裁判' : '运动员' }}，可以使用前台功能。</p>
          <el-divider></el-divider>
          <div class="feature-list">
            <h3>前台功能</h3>
            <ul>
              <li v-if="user || true">查看比赛信息</li>
              <li v-if="user || true">查看积分榜</li>
              <li v-if="user || true">查看射手榜</li>
              <li v-if="user">查看球队信息</li>
              <li v-if="user">查看球员信息</li>
              <li v-if="user && user.role !== 'user'">提交赛事申请</li>
              <li v-if="user && user.role === 'player'">查看个人比赛记录</li>
            </ul>
          </div>
          <div class="action-buttons">
            <el-button type="primary" @click="goToMatchList">查看比赛</el-button>
            <el-button type="success" @click="goToStandings">查看积分榜</el-button>
            <el-button type="warning" @click="goToTopScorers">查看射手榜</el-button>
            <el-button type="info" @click="showLoginDialog" v-if="!user">登录/注册</el-button>
          </div>
        </div>
      </el-card>
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
  name: 'Frontend',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
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
      }
    }
  },
  mounted() {
    this.user = this.$store.state.user
  },
  methods: {
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
            if (response.user.role === 'admin') {
              this.$router.push('/')
            }
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
    goToMatchList() {
      this.$message.info('比赛列表功能正在开发中')
    },
    goToStandings() {
      this.$message.info('积分榜功能正在开发中')
    },
    goToTopScorers() {
      this.$message.info('射手榜功能正在开发中')
    }
  }
}
</script>

<style scoped>
.frontend-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.frontend-header {
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

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.frontend-content {
  padding-top: 80px;
  padding-bottom: 40px;
}

.welcome-card {
  max-width: 800px;
  margin: 0 auto;
}

.welcome-content {
  text-align: center;
  padding: 40px 20px;
}

.welcome-content h2 {
  margin: 20px 0;
  font-size: 28px;
  color: #303133;
}

.welcome-content p {
  font-size: 16px;
  color: #606266;
  margin: 10px 0;
}

.feature-list {
  text-align: left;
  margin: 30px 0;
  padding: 20px;
  background-color: #f9fafc;
  border-radius: 8px;
}

.feature-list h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #303133;
}

.feature-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.feature-list li {
  padding: 8px 0;
  color: #606266;
}

.feature-list li:before {
  content: "•";
  color: #409EFF;
  font-weight: bold;
  display: inline-block;
  width: 1em;
  margin-left: -1em;
}

.action-buttons {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  gap: 15px;
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
