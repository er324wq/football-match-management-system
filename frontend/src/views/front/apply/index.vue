<template>
  <div class="front-apply">
    <div class="apply-header">
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
    <div class="apply-content">
      <div v-if="!user" class="login-prompt">
        <el-card shadow="hover">
          <div class="prompt-content">
            <i class="el-icon-user" style="font-size: 64px; color: #409EFF; margin-bottom: 20px;"></i>
            <h2>请先登录</h2>
            <p>登录后可以提交身份申请</p>
            <el-button type="primary" size="large" @click="showLoginDialog">立即登录</el-button>
          </div>
        </el-card>
      </div>
      <div v-else class="apply-container">
        <el-card shadow="hover">
          <div class="apply-header-info">
            <h2>身份申请</h2>
            <p>请选择您要申请的身份类型</p>
          </div>
          <div class="apply-types">
            <el-row :gutter="20">
              <el-col :span="8">
                <el-card shadow="hover" @click="selectApplyType('player')" :class="{ active: selectedType === 'player' }">
                  <div class="apply-type-card">
                    <div class="apply-icon">
                      <i class="el-icon-s-custom"></i>
                    </div>
                    <h3>运动员</h3>
                    <p>申请成为系统认证的运动员</p>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card shadow="hover" @click="selectApplyType('referee')" :class="{ active: selectedType === 'referee' }">
                  <div class="apply-type-card">
                    <div class="apply-icon">
                      <i class="el-icon-s-flag"></i>
                    </div>
                    <h3>裁判</h3>
                    <p>申请成为系统认证的裁判</p>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card shadow="hover" @click="selectApplyType('team')" :class="{ active: selectedType === 'team' }">
                  <div class="apply-type-card">
                    <div class="apply-icon">
                      <i class="el-icon-football"></i>
                    </div>
                    <h3>球队</h3>
                    <p>申请成为系统认证的球队</p>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <div class="apply-form" v-if="selectedType">
            <h3>{{ getTypeTitle(selectedType) }}申请</h3>
            <el-form ref="applyForm" :model="applyForm" :rules="applyRules" label-width="120px">
              <el-form-item label="姓名" prop="name">
                <el-input v-model="applyForm.name" placeholder="请输入姓名"></el-input>
              </el-form-item>
              <el-form-item label="年龄" prop="age" v-if="selectedType === 'player' || selectedType === 'referee'">
                <el-input type="number" v-model="applyForm.age" placeholder="请输入年龄"></el-input>
              </el-form-item>
              <el-form-item label="性别" prop="gender" v-if="selectedType === 'player' || selectedType === 'referee'">
                <el-select v-model="applyForm.gender" placeholder="请选择性别">
                  <el-option label="男" value="male"></el-option>
                  <el-option label="女" value="female"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="球队名称" prop="teamName" v-if="selectedType === 'player'">
                <el-input v-model="applyForm.teamName" placeholder="请输入球队名称"></el-input>
              </el-form-item>
              <el-form-item label="位置" prop="position" v-if="selectedType === 'player'">
                <el-select v-model="applyForm.position" placeholder="请选择位置">
                  <el-option label="前锋" value="forward"></el-option>
                  <el-option label="中场" value="midfielder"></el-option>
                  <el-option label="后卫" value="defender"></el-option>
                  <el-option label="守门员" value="goalkeeper"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="裁判等级" prop="level" v-if="selectedType === 'referee'">
                <el-select v-model="applyForm.level" placeholder="请选择裁判等级">
                  <el-option label="国家级" value="national"></el-option>
                  <el-option label="国际级" value="international"></el-option>
                  <el-option label="省级" value="provincial"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="球队名称" prop="teamName" v-if="selectedType === 'team'">
                <el-input v-model="applyForm.teamName" placeholder="请输入球队名称"></el-input>
              </el-form-item>
              <el-form-item label="成立时间" prop="founded" v-if="selectedType === 'team'">
                <el-date-picker type="date" v-model="applyForm.founded" placeholder="请选择成立时间" format="yyyy-MM-dd"></el-date-picker>
              </el-form-item>
              <el-form-item label="联系电话" prop="phone">
                <el-input v-model="applyForm.phone" placeholder="请输入联系电话"></el-input>
              </el-form-item>
              <el-form-item label="电子邮箱" prop="email">
                <el-input v-model="applyForm.email" placeholder="请输入电子邮箱"></el-input>
              </el-form-item>
              <el-form-item label="申请理由" prop="reason">
                <el-input type="textarea" v-model="applyForm.reason" placeholder="请输入申请理由" rows="4"></el-input>
              </el-form-item>
              <el-form-item label="上传证明材料">
                <el-upload
                  class="upload-demo"
                  :action="uploadUrl"
                  :on-success="handleUploadSuccess"
                  :on-error="handleUploadError"
                  :limit="3"
                  :file-list="fileList"
                >
                  <el-button type="primary">点击上传</el-button>
                  <div slot="tip" class="el-upload__tip">最多上传3个文件，支持jpg、png、pdf格式</div>
                </el-upload>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitApply">提交申请</el-button>
                <el-button @click="resetForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div class="apply-history" v-if="user">
            <h3>申请历史</h3>
            <el-table :data="applyHistory" style="width: 100%">
              <el-table-column prop="type" label="申请类型" width="120">
                <template slot-scope="scope">
                  {{ getTypeTitle(scope.row.type) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template slot-scope="scope">
                  <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="applyTime" label="申请时间" width="180"></el-table-column>
              <el-table-column prop="reason" label="申请理由" show-overflow-tooltip></el-table-column>
              <el-table-column label="操作" width="120">
                <template slot-scope="scope">
                  <el-button type="text" @click="viewApplyDetails(scope.row.id)">查看详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
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
  name: 'FrontApply',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      activeMenu: 'apply',
      user: null,
      dialogVisible: false,
      isRegister: false,
      loading: false,
      selectedType: '',
      uploadUrl: '/api/upload',
      fileList: [],
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
      applyForm: {
        name: '',
        age: '',
        gender: '',
        teamName: '',
        position: '',
        level: '',
        founded: '',
        phone: '',
        email: '',
        reason: ''
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
      applyRules: {
        name: [
          { required: true, message: '请输入姓名', trigger: 'blur' }
        ],
        age: [
          { required: true, message: '请输入年龄', trigger: 'blur' },
          { type: 'number', min: 1, message: '年龄必须大于0', trigger: 'blur' }
        ],
        gender: [
          { required: true, message: '请选择性别', trigger: 'blur' }
        ],
        teamName: [
          { required: true, message: '请输入球队名称', trigger: 'blur' }
        ],
        position: [
          { required: true, message: '请选择位置', trigger: 'blur' }
        ],
        level: [
          { required: true, message: '请选择裁判等级', trigger: 'blur' }
        ],
        founded: [
          { required: true, message: '请选择成立时间', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入电子邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入有效的电子邮箱', trigger: 'blur' }
        ],
        reason: [
          { required: true, message: '请输入申请理由', trigger: 'blur' }
        ]
      },
      applyHistory: [
        {
          id: 1,
          type: 'player',
          status: 'pending',
          applyTime: '2026-03-25 10:30',
          reason: '我是一名足球运动员，希望加入系统'
        },
        {
          id: 2,
          type: 'referee',
          status: 'approved',
          applyTime: '2026-03-20 15:45',
          reason: '我是一名国家级裁判，希望加入系统'
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
    selectApplyType(type) {
      this.selectedType = type
      this.resetForm()
    },
    getTypeTitle(type) {
      const typeMap = {
        player: '运动员',
        referee: '裁判',
        team: '球队'
      }
      return typeMap[type] || ''
    },
    getStatusType(status) {
      const statusMap = {
        pending: 'warning',
        approved: 'success',
        rejected: 'danger'
      }
      return statusMap[status] || 'info'
    },
    getStatusText(status) {
      const statusMap = {
        pending: '待审核',
        approved: '已通过',
        rejected: '已拒绝'
      }
      return statusMap[status] || ''
    },
    handleUploadSuccess(response, file, fileList) {
      this.$message.success('文件上传成功')
    },
    handleUploadError(error, file, fileList) {
      this.$message.error('文件上传失败')
    },
    submitApply() {
      this.$refs.applyForm.validate(async (valid) => {
        if (valid) {
          // 实际项目中这里会调用API提交申请
          this.$message.success('申请提交成功，等待审核')
          this.resetForm()
        }
      })
    },
    resetForm() {
      this.applyForm = {
        name: '',
        age: '',
        gender: '',
        teamName: '',
        position: '',
        level: '',
        founded: '',
        phone: '',
        email: '',
        reason: ''
      }
      this.fileList = []
      if (this.$refs.applyForm) {
        this.$refs.applyForm.clearValidate()
      }
    },
    viewApplyDetails(applyId) {
      // 实际项目中这里会跳转到申请详情页面
      this.$message.info(`查看申请ID: ${applyId} 的详情`)
    }
  }
}
</script>

<style scoped>
.front-apply {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.apply-header {
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

.apply-content {
  padding-top: 80px;
  padding-bottom: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding-left: 20px;
  padding-right: 20px;
}

.login-prompt {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 500px;
}

.prompt-content {
  text-align: center;
  padding: 40px;
}

.apply-header-info {
  text-align: center;
  margin-bottom: 30px;
}

.apply-header-info h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
  color: #303133;
}

.apply-header-info p {
  margin: 0;
  font-size: 16px;
  color: #606266;
}

.apply-types {
  margin-bottom: 30px;
}

.apply-type-card {
  text-align: center;
  padding: 30px 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.apply-type-card:hover {
  transform: translateY(-5px);
}

.apply-icon {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 20px;
}

.apply-type-card h3 {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #303133;
}

.apply-type-card p {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.apply-type-card.active {
  border: 2px solid #409EFF;
  border-radius: 8px;
}

.apply-form {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f9fafc;
  border-radius: 8px;
}

.apply-form h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #303133;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.apply-history {
  margin-top: 30px;
}

.apply-history h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #303133;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
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
