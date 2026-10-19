<template>
  <div class="front-notice">
    <div class="notice-header">
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
    <div class="notice-content">
      <div class="notice-filter">
        <el-form :inline="true" :model="filterForm" class="demo-form-inline">
          <el-form-item>
            <el-input v-model="filterForm.keyword" placeholder="搜索公告标题" prefix-icon="el-icon-search"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="notice-list">
        <el-card v-for="notice in notices" :key="notice.id" shadow="hover">
          <div class="notice-card">
            <div class="notice-header-info">
              <h3 class="notice-title" @click="viewNoticeDetails(notice.id)">{{ notice.title }}</h3>
              <span class="notice-time">{{ notice.time }}</span>
            </div>
            <div class="notice-content-preview">{{ notice.content.substring(0, 150) }}...</div>
            <div class="notice-footer">
              <span class="notice-author">{{ notice.author }}</span>
              <el-button type="primary" size="small" @click="viewNoticeDetails(notice.id)">查看详情</el-button>
            </div>
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
export default {
  name: 'FrontNotice',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      activeMenu: 'notice',
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
        keyword: ''
      },
      currentPage: 1,
      pageSize: 10,
      total: 50,
      notices: [
        {
          id: 1,
          title: '关于2026赛季赛程安排的通知',
          content: '2026赛季的赛程安排已经确定，联赛将于4月1日正式开始，共有20支球队参加，分为38轮比赛。揭幕战将由上赛季冠军皇家马德里对阵巴塞罗那，比赛时间为4月1日晚上8点。请各球队做好准备，确保比赛顺利进行。同时，我们也欢迎球迷朋友们前往现场观看比赛，为自己支持的球队加油助威。',
          time: '2026-03-30',
          author: '联赛组委会'
        },
        {
          id: 2,
          title: '球迷嘉年华活动报名开始',
          content: '为了增强球迷与球队之间的互动，我们将于4月15日举办球迷嘉年华活动，活动包括球员见面会、签名会、足球技能挑战等环节。所有球迷朋友都可以报名参加，报名时间从即日起至4月10日截止。名额有限，先到先得。活动当天，我们还将抽取幸运球迷，赠送球队签名球衣和球票。',
          time: '2026-03-28',
          author: '球迷协会'
        },
        {
          id: 3,
          title: '新赛季球员转会信息',
          content: '在转会窗口关闭前，各球队完成了一系列重要转会。皇家马德里签下了年轻前锋哈兰德，巴塞罗那引进了中场大将德布劳内，利物浦则从拜仁慕尼黑签下了后卫阿拉巴。这些转会将对新赛季的联赛格局产生重要影响，让我们拭目以待。',
          time: '2026-03-25',
          author: '转会委员会'
        },
        {
          id: 4,
          title: '关于加强赛场纪律的通知',
          content: '为了维护比赛秩序，保障比赛的公平公正，联赛组委会决定加强赛场纪律管理。从新赛季开始，将对球员的恶意犯规、辱骂裁判等行为进行严厉处罚，包括罚款、停赛等措施。同时，我们也呼吁球迷朋友们文明观赛，共同营造良好的赛场氛围。',
          time: '2026-03-20',
          author: '联赛组委会'
        },
        {
          id: 5,
          title: '联赛官方APP上线',
          content: '为了方便球迷朋友们及时了解联赛动态，我们开发了联赛官方APP，现已在各大应用商店上线。通过APP，球迷朋友们可以查看实时比分、赛程安排、球员数据等信息，还可以参与互动活动，赢取精美礼品。欢迎大家下载使用。',
          time: '2026-03-15',
          author: '技术部'
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
    handleSearch() {
      // 实际项目中这里会调用API获取数据
      this.$message.info('查询功能正在开发中')
    },
    resetForm() {
      this.filterForm = {
        keyword: ''
      }
    },
    handleSizeChange(size) {
      this.pageSize = size
      // 实际项目中这里会调用API获取数据
    },
    handleCurrentChange(current) {
      this.currentPage = current
      // 实际项目中这里会调用API获取数据
    },
    viewNoticeDetails(noticeId) {
      // 实际项目中这里会跳转到公告详情页面
      this.$message.info(`查看公告ID: ${noticeId} 的详情`)
    }
  }
}
</script>

<style scoped>
.front-notice {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.notice-header {
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

.notice-content {
  padding-top: 80px;
  padding-bottom: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding-left: 20px;
  padding-right: 20px;
}

.notice-filter {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.notice-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(450px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.notice-card {
  padding: 20px;
}

.notice-header-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.notice-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
  cursor: pointer;
  flex: 1;
  margin-right: 20px;
}

.notice-title:hover {
  color: #409EFF;
}

.notice-time {
  font-size: 14px;
  color: #909399;
  white-space: nowrap;
}

.notice-content-preview {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 15px;
}

.notice-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.notice-author {
  font-size: 14px;
  color: #909399;
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
