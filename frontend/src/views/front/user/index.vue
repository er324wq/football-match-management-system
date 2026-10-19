<template>
  <div class="front-user">
    <header class="top-nav">
      <div class="header-content">
        <div class="logo"><i class="el-icon-football"></i><span>足球比赛管理系统</span></div>
        <div class="nav-menu">
          <el-menu :default-active="activeMenu" mode="horizontal" @select="handleSelect">
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
          <span v-if="user">{{ user.nickname || user.username }}</span>
          <span v-else>游客</span>
          <el-button type="text" v-if="user" @click="handleLogout">退出</el-button>
          <el-button type="text" v-else @click="$router.push('/login')">登录</el-button>
        </div>
      </div>
    </header>

    <main class="content">
      <el-card v-if="!user">
        <h2>请先登录</h2>
        <el-button type="primary" @click="$router.push('/login')">前往登录</el-button>
      </el-card>

      <template v-else>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-card>
              <div class="card-head">
                <h3>个性化设置</h3>
              </div>
              <el-form :model="settingsForm" label-width="70px">
                <el-form-item label="昵称">
                  <el-input v-model="settingsForm.nickname" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="settingsForm.phone" placeholder="11位手机号" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="settingsForm.email" placeholder="常用邮箱" />
                </el-form-item>
                <el-form-item label="头像URL">
                  <el-input v-model="settingsForm.avatar" placeholder="可选" />
                </el-form-item>
                <el-form-item label="个性签名">
                  <el-input v-model="settingsForm.signature" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingSettings" @click="saveSettings">保存设置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>

          <el-col :span="16">
            <el-card>
              <div class="card-head">
                <h3>消息通知</h3>
                <div>
                  <el-badge :value="unreadCount" :hidden="unreadCount === 0" style="margin-right:8px" />
                  <el-button size="mini" @click="fetchNotifications">刷新</el-button>
                  <el-button size="mini" type="primary" @click="markAllAsRead">全部已读</el-button>
                </div>
              </div>
              <el-table :data="notifications" size="small" @row-click="openNotificationDetail">
                <el-table-column label="状态" width="70">
                  <template slot-scope="scope">
                    <el-tag size="mini" :type="scope.row.isRead ? 'info' : 'danger'">{{ scope.row.isRead ? '已读' : '未读' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="title" label="标题" min-width="180" />
                <el-table-column prop="type" label="类型" width="100" />
                <el-table-column prop="createTime" label="时间" width="180" />
                <el-table-column label="操作" width="100">
                  <template slot-scope="scope">
                    <el-button type="text" @click.stop="openNotificationDetail(scope.row)">查看</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>

            <el-card style="margin-top:16px">
              <div class="card-head">
                <h3>个人成绩</h3>
              </div>
              <div v-if="personalPerformance.hasResult" class="performance-grid">
                <div class="performance-item">
                  <div class="label">球员姓名</div>
                  <div class="value">{{ personalPerformance.playerName || '-' }}</div>
                </div>
                <div class="performance-item">
                  <div class="label">所在球队</div>
                  <div class="value">{{ personalPerformance.teamName || '-' }}</div>
                </div>
                <div class="performance-item">
                  <div class="label">进球得分</div>
                  <div class="value">{{ personalPerformance.goals || 0 }}</div>
                </div>
                <div class="performance-item">
                  <div class="label">射手榜排名</div>
                  <div class="value">{{ personalPerformance.scorerRank ? `第 ${personalPerformance.scorerRank} 名` : '暂无' }}</div>
                </div>
                <div class="performance-item">
                  <div class="label">球队积分榜排名</div>
                  <div class="value">{{ personalPerformance.teamRank ? `第 ${personalPerformance.teamRank} 名` : '暂无' }}</div>
                </div>
                <div class="performance-item">
                  <div class="label">球队积分</div>
                  <div class="value">{{ personalPerformance.teamPoints != null ? personalPerformance.teamPoints : '暂无' }}</div>
                </div>
              </div>
              <el-empty v-else description="暂无成绩" :image-size="90" />
            </el-card>
          </el-col>
        </el-row>

        <el-card style="margin-top:16px">
          <div class="card-head"><h3>我的申请</h3></div>
          <el-table :data="applications" size="small" @row-click="openApplicationDetail">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="type" label="类型" width="140" />
            <el-table-column prop="displayName" label="名称/目标" min-width="220" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column prop="createTime" label="提交时间" width="180" />
            <el-table-column label="操作" width="100">
              <template slot-scope="scope">
                <el-button type="text" @click.stop="openApplicationDetail(scope.row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </main>

    <el-dialog title="通知详情" :visible.sync="notificationDetailVisible" width="560px">
      <el-descriptions :column="1" border v-if="notificationDetail">
        <el-descriptions-item label="标题">{{ notificationDetail.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ notificationDetail.type }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ notificationDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ notificationDetail.content }}</el-descriptions-item>
      </el-descriptions>
      <span slot="footer"><el-button @click="notificationDetailVisible = false">关闭</el-button></span>
    </el-dialog>

    <el-dialog title="申请详情" :visible.sync="applicationDetailVisible" width="620px">
      <el-descriptions :column="1" border v-if="applicationDetail">
        <el-descriptions-item label="申请ID">{{ applicationDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ applicationDetail.type }}</el-descriptions-item>
        <el-descriptions-item label="目标ID">{{ applicationDetail.targetId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ applicationDetail.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="原因">{{ applicationDetail.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ applicationDetail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ applicationDetail.status }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ applicationDetail.createTime }}</el-descriptions-item>
      </el-descriptions>
      <span slot="footer"><el-button @click="applicationDetailVisible = false">关闭</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import * as notificationApi from '@/api/notification'
import * as applicationApi from '@/api/application'
import * as userApi from '@/api/user'
import * as statisticsApi from '@/api/statistics'

export default {
  name: 'FrontUser',
  data() {
    return {
      activeMenu: 'user',
      notifications: [],
      unreadCount: 0,
      applications: [],
      notificationDetailVisible: false,
      applicationDetailVisible: false,
      notificationDetail: null,
      applicationDetail: null,
      settingsForm: {
        nickname: '',
        phone: '',
        email: '',
        avatar: '',
        signature: ''
      },
      savingSettings: false,
      personalPerformance: {
        hasResult: false,
        playerName: '',
        teamName: '',
        goals: 0,
        scorerRank: null,
        teamRank: null,
        teamPoints: null
      }
    }
  },
  computed: {
    user() {
      return this.$store.state.user
    }
  },
  mounted() {
    if (this.user && this.user.id) {
      this.syncUserProfile()
      this.fetchNotifications()
      this.fetchApplications()
      this.fetchPersonalPerformance()
    }
  },
  methods: {
    handleSelect(key) {
      this.$router.push(`/front/${key}`)
    },
    async fetchPersonalPerformance() {
      if (!this.user || !this.user.id) return
      try {
        const data = await statisticsApi.getPersonalPerformance(this.user.id)
        this.personalPerformance = {
          hasResult: !!(data && data.hasResult),
          playerName: data && data.playerName ? data.playerName : '',
          teamName: data && data.teamName ? data.teamName : '',
          goals: data && data.goals != null ? data.goals : 0,
          scorerRank: data && data.scorerRank != null ? data.scorerRank : null,
          teamRank: data && data.teamRank != null ? data.teamRank : null,
          teamPoints: data && data.teamPoints != null ? data.teamPoints : null
        }
      } catch (e) {
        this.personalPerformance = {
          hasResult: false,
          playerName: '',
          teamName: '',
          goals: 0,
          scorerRank: null,
          teamRank: null,
          teamPoints: null
        }
      }
    },
    async fetchNotifications() {
      if (!this.user || !this.user.id) return
      try {
        const list = await notificationApi.getUserNotifications(this.user.id)
        this.notifications = Array.isArray(list) ? list : []
        this.unreadCount = this.notifications.filter(item => !item.isRead).length
      } catch (e) {
        this.notifications = []
        this.unreadCount = 0
      }
    },
    async markAllAsRead() {
      if (!this.user || !this.user.id) return
      await notificationApi.markAllAsRead(this.user.id)
      this.notifications = this.notifications.map(item => ({ ...item, isRead: true }))
      this.unreadCount = 0
    },
    async openNotificationDetail(row) {
      this.notificationDetail = row
      this.notificationDetailVisible = true
      if (!row.isRead) {
        try {
          await notificationApi.markAsRead(row.id)
          row.isRead = true
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        } catch (e) {}
      }
    },
    async fetchApplications() {
      if (!this.user || !this.user.id) return
      try {
        const list = await applicationApi.getUserApplications(this.user.id)
        this.applications = Array.isArray(list) ? list.map(item => ({
          ...item,
          displayName: item.name || item.reason || (item.targetId ? `目标#${item.targetId}` : '-')
        })) : []
      } catch (e) {
        this.applications = []
      }
    },
    openApplicationDetail(row) {
      this.applicationDetail = row
      this.applicationDetailVisible = true
    },
    async saveSettings() {
      if (!this.user || !this.user.id) return
      if (!this.settingsForm.phone || !/^1\d{10}$/.test(this.settingsForm.phone)) {
        this.$message.error('请输入有效的11位手机号')
        return
      }
      if (!this.settingsForm.email || !/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/.test(this.settingsForm.email)) {
        this.$message.error('请输入有效邮箱')
        return
      }
      this.savingSettings = true
      try {
        await userApi.updateUser({
          id: this.user.id,
          nickname: this.settingsForm.nickname,
          phone: this.settingsForm.phone,
          email: this.settingsForm.email,
          avatar: this.settingsForm.avatar,
          signature: this.settingsForm.signature
        })
        const refreshedUser = await userApi.getUserById(this.user.id)
        const nextUser = {
          ...this.user,
          ...refreshedUser
        }
        this.$store.commit('SET_USER', nextUser)
        this.settingsForm = {
          nickname: refreshedUser.nickname || '',
          phone: refreshedUser.phone || '',
          email: refreshedUser.email || '',
          avatar: refreshedUser.avatar || '',
          signature: refreshedUser.signature || ''
        }
        this.$message.success('个性化设置已保存')
      } catch (e) {
        this.$message.error(e.message || '保存设置失败')
      } finally {
        this.savingSettings = false
      }
    },
    async handleLogout() {
      await this.$store.dispatch('logout')
      this.$router.push('/front/home')
    }
  }
}
</script>

<style scoped>
.front-user { min-height: 100vh; background: #f5f7fa; }
.top-nav { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.header-content { max-width: 1200px; margin: 0 auto; height: 60px; padding: 0 20px; display: flex; align-items: center; }
.logo { display: flex; align-items: center; font-weight: 700; }
.logo i { margin-right: 8px; color: #409eff; }
.nav-menu { flex: 1; margin: 0 24px; }
.user-info { display: flex; align-items: center; gap: 10px; }
.content { max-width: 1200px; margin: 20px auto; padding: 0 16px; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.performance-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.performance-item { background: #f8fafc; border: 1px solid #eef2f7; border-radius: 8px; padding: 12px; }
.performance-item .label { color: #909399; font-size: 12px; margin-bottom: 6px; }
.performance-item .value { color: #303133; font-size: 18px; font-weight: 600; }
</style>
    async syncUserProfile() {
      if (!this.user || !this.user.id) return
      try {
        const userDetail = await userApi.getUserById(this.user.id)
        this.settingsForm = {
          nickname: userDetail.nickname || '',
          phone: userDetail.phone || '',
          email: userDetail.email || '',
          avatar: userDetail.avatar || '',
          signature: userDetail.signature || ''
        }
        this.$store.commit('SET_USER', {
          ...this.user,
          ...userDetail
        })
      } catch (e) {
        this.settingsForm = {
          nickname: this.user.nickname || '',
          phone: this.user.phone || '',
          email: this.user.email || '',
          avatar: this.user.avatar || '',
          signature: this.user.signature || ''
        }
      }
    },
