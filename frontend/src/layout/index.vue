<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <el-icon class="logo-icon"><i class="el-icon-football"></i></el-icon>
        <span>足球比赛管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical-demo"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/admin/dashboard">
          <i class="el-icon-s-home"></i>
          <span>首页</span>
        </el-menu-item>
        <el-submenu index="system">
          <template slot="title">
            <i class="el-icon-setting"></i>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/admin/user">
            <i class="el-icon-user"></i>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/application">
            <i class="el-icon-document"></i>
            <span>申请管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/performance">
            <i class="el-icon-data-analysis"></i>
            <span>性能管理</span>
          </el-menu-item>
        </el-submenu>
        <el-submenu index="basic">
          <template slot="title">
            <i class="el-icon-menu"></i>
            <span>基础信息管理</span>
          </template>
          <el-menu-item index="/admin/team">
            <i class="el-icon-football"></i>
            <span>球队管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/player">
            <i class="el-icon-s-custom"></i>
            <span>球员管理</span>
          </el-menu-item>
        </el-submenu>
        <el-submenu index="match">
          <template slot="title">
            <i class="el-icon-date"></i>
            <span>赛事管理</span>
          </template>
          <el-menu-item index="/admin/match">
            <i class="el-icon-time"></i>
            <span>比赛管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/match-result">
            <i class="el-icon-finished"></i>
            <span>比赛结果管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/goal-record">
            <i class="el-icon-s-flag"></i>
            <span>比赛记录管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/match-application">
            <i class="el-icon-document"></i>
            <span>赛事申请管理</span>
          </el-menu-item>
        </el-submenu>
        <el-submenu index="statistics">
          <template slot="title">
            <i class="el-icon-data-analysis"></i>
            <span>数据统计</span>
          </template>
          <el-menu-item index="/admin/standings">
            <i class="el-icon-data-line"></i>
            <span>积分榜</span>
          </el-menu-item>
          <el-menu-item index="/admin/top-scorers">
            <i class="el-icon-trophy"></i>
            <span>射手榜</span>
          </el-menu-item>
        </el-submenu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="current-time">{{ currentTime }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              {{ user ? user.nickname : '管理员' }}
              <i class="el-icon-arrow-down"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
export default {
  name: 'Layout',
  data() {
    return {
      activeMenu: '/dashboard',
      currentTime: '',
      timeInterval: null,
      matchStatusInterval: null,
      isLoggingOut: false
    }
  },
  computed: {
    user() {
      return this.$store.state.user
    }
  },
  mounted() {
    this.updateActiveMenu()
    this.updateTime()
    this.timeInterval = setInterval(this.updateTime, 1000)
    this.matchStatusInterval = setInterval(this.updateMatchStatuses, 60000) // 每分钟检查一次比赛状态
  },
  watch: {
    $route() {
      this.updateActiveMenu()
    }
  },
  methods: {
    updateActiveMenu() {
      this.activeMenu = this.$route.path
    },
    updateTime() {
      const now = new Date()
      const year = now.getFullYear()
      const month = String(now.getMonth() + 1).padStart(2, '0')
      const day = String(now.getDate()).padStart(2, '0')
      const hours = String(now.getHours()).padStart(2, '0')
      const minutes = String(now.getMinutes()).padStart(2, '0')
      const seconds = String(now.getSeconds()).padStart(2, '0')
      this.currentTime = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    },
    async updateMatchStatuses() {
      try {
        const response = await this.$http.get('/api/match/update-status')
        console.log('比赛状态更新成功:', response)
      } catch (error) {
        console.error('更新比赛状态失败:', error)
      }
    },
    async handleCommand(command) {
      if (command === 'logout') {
        this.$confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          // 开始退出登录，禁用所有操作
          this.isLoggingOut = true
          try {
            // 清除定时任务
            if (this.timeInterval) {
              clearInterval(this.timeInterval)
            }
            if (this.matchStatusInterval) {
              clearInterval(this.matchStatusInterval)
            }
            // 退出登录
            await this.$store.dispatch('logout')
            // 跳转到登录页
            this.$router.push('/login')
            this.$message.success('退出成功')
          } catch (error) {
            console.error('退出登录失败:', error)
            this.$message.error('退出登录失败')
          } finally {
            // 退出登录完成，启用操作
            this.isLoggingOut = false
          }
        })
      }
    }
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  background-color: #304156;
  overflow-y: auto;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid #1f2d3d;
}

.logo-icon {
  font-size: 24px;
  margin-right: 8px;
  color: #409EFF;
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.current-time {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #606266;
  padding: 8px 16px;
  border-radius: 4px;
  transition: all 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.user-info i {
  margin-left: 4px;
  font-size: 12px;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}

.el-menu-vertical-demo:not(.el-menu--collapse) {
  width: 200px;
  min-height: 400px;
}

.el-menu {
  border-right: none;
}
</style>
