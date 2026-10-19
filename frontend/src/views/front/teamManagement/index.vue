<template>
  <div class="team-management">
    <header class="top-nav">
      <div class="header-content">
        <div class="logo">
          <i class="el-icon-football"></i>
          <span>足球比赛管理系统</span>
        </div>
        <div class="nav-menu">
          <el-menu :default-active="activeMenu" mode="horizontal" @select="handleMenuSelect">
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

    <div class="main-content">
      <el-card v-if="!user" class="main-card">
        <h2>请先登录后管理球队</h2>
        <el-button type="primary" @click="$router.push('/login')">前往登录</el-button>
      </el-card>

      <template v-else>
        <el-card class="main-card" v-if="teams.length === 0">
          <h2>你还没有球队</h2>
          <p class="hint">可以申请加入球队，或提交创建球队申请。</p>
          <div class="actions">
            <el-button type="primary" @click="openJoinDialog">加入球队</el-button>
            <el-button @click="applyDialogVisible = true">申请创建球队</el-button>
            <el-button @click="refreshAll">刷新</el-button>
          </div>
          <el-alert v-if="pendingCreateApplication" title="你有创建球队申请正在审核中" type="warning" :closable="false" style="margin-top: 10px" />
          <el-alert v-if="pendingJoinApplication" title="你有入队申请正在处理中" type="info" :closable="false" style="margin-top: 10px" />
        </el-card>

        <div v-else class="manage-layout">
          <el-card class="team-list-card">
            <div class="card-header">
              <h3>我的球队</h3>
              <el-button size="mini" type="primary" @click="triggerUpload">上传图片</el-button>
              <input ref="coverInput" type="file" accept="image/*" style="display:none" @change="handleCoverUpload">
            </div>
            <div class="cover-box">
              <img :src="activeTeamCover" alt="team-cover">
            </div>
          </el-card>

          <el-card class="team-detail-card" v-if="activeTeam">
            <div class="card-header">
              <h3>{{ activeTeam.name }}</h3>
              <div>
                <el-button size="mini" @click="openEditDialog(activeTeam)">修改球队信息</el-button>
                <el-button size="mini" type="danger" @click="deleteCurrentTeam(activeTeam)">解散球队</el-button>
              </div>
            </div>

            <el-descriptions :column="1" border>
              <el-descriptions-item label="球队名称">{{ activeTeam.name }}</el-descriptions-item>
              <el-descriptions-item label="城市">{{ activeTeam.city || '-' }}</el-descriptions-item>
              <el-descriptions-item label="教练">{{ activeTeam.coach || '-' }}</el-descriptions-item>
              <el-descriptions-item label="球队简介">{{ activeTeam.description || '-' }}</el-descriptions-item>
            </el-descriptions>

            <div class="actions" style="margin-top:16px">
              <el-button type="success" @click="openMatchApplyDialog">战队报名</el-button>
              <el-button type="warning" @click="openLineupDialog">球队安排</el-button>
              <el-button type="primary" @click="openApplicationDialog">入队申请管理</el-button>
              <el-button @click="refreshAll">刷新数据</el-button>
            </div>
          </el-card>
        </div>

        <el-card class="all-team-card" v-if="teams.length > 0">
          <div class="card-header">
            <h3>队员信息</h3>
            <span class="sub-text">当前战队全部成员</span>
          </div>
          <el-table :data="memberTableData" border size="small" v-loading="memberTableLoading">
            <el-table-column prop="name" label="姓名" min-width="120" />
            <el-table-column prop="position" label="位置" width="120" />
            <el-table-column prop="age" label="年龄" width="90" />
            <el-table-column prop="jerseyNumber" label="球衣号" width="100" />
            <el-table-column prop="remark" label="备注" min-width="180" />
          </el-table>
        </el-card>
      </template>
    </div>

    <el-dialog title="修改球队信息" :visible.sync="teamDialogVisible" width="520px">
      <el-form ref="teamFormRef" :model="teamForm" :rules="teamRules" label-width="90px">
        <el-form-item label="球队名称" prop="name"><el-input v-model="teamForm.name" /></el-form-item>
        <el-form-item label="所在城市" prop="city"><el-input v-model="teamForm.city" /></el-form-item>
        <el-form-item label="主教练" prop="coach"><el-input v-model="teamForm.coach" /></el-form-item>
        <el-form-item label="球队简介"><el-input v-model="teamForm.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="teamDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingTeam" @click="submitTeam">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog title="申请创建球队" :visible.sync="applyDialogVisible" width="520px">
      <el-form ref="applyFormRef" :model="applyForm" :rules="teamRules" label-width="90px">
        <el-form-item label="球队名称" prop="name"><el-input v-model="applyForm.name" /></el-form-item>
        <el-form-item label="所在城市" prop="city"><el-input v-model="applyForm.city" /></el-form-item>
        <el-form-item label="主教练" prop="coach"><el-input v-model="applyForm.coach" /></el-form-item>
        <el-form-item label="球队简介"><el-input v-model="applyForm.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingApply" @click="submitCreateApplication">提交申请</el-button>
      </div>
    </el-dialog>

    <el-dialog title="加入球队" :visible.sync="joinDialogVisible" width="520px">
      <el-form ref="joinFormRef" :model="joinForm" :rules="joinRules" label-width="90px">
        <el-form-item label="目标球队" prop="teamId">
          <el-select v-model="joinForm.teamId" placeholder="请选择球队" style="width:100%">
            <el-option v-for="item in joinTeams" :key="item.id" :label="`${item.name}${item.city ? ' (' + item.city + ')' : ''}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请位置" prop="position">
          <el-select v-model="joinForm.position" placeholder="请选择位置" style="width:100%">
            <el-option v-for="item in positionOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请说明"><el-input v-model="joinForm.reason" type="textarea" :rows="3" placeholder="选填" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingJoin" @click="submitJoinApplication">提交入队申请</el-button>
      </div>
    </el-dialog>

    <el-dialog title="战队报名" :visible.sync="matchApplyDialogVisible" width="620px">
      <el-form ref="matchApplyFormRef" :model="matchApplyForm" :rules="matchApplyRules" label-width="90px">
        <el-form-item label="选择赛事" prop="eventId">
          <el-select v-model="matchApplyForm.eventId" placeholder="请选择可报名赛事" style="width:100%">
            <el-option
              v-for="event in matchEvents"
              :key="event.id"
              :label="`${event.name || ('赛事#' + event.id)} | ${event.time || '-'} | ${event.location || '-'}`"
              :value="event.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="报名说明"><el-input v-model="matchApplyForm.reason" type="textarea" :rows="3" placeholder="选填" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="matchApplyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingMatchApply" @click="submitMatchApplication">提交战队报名</el-button>
      </div>
    </el-dialog>

    <el-dialog title="球队安排" :visible.sync="lineupDialogVisible" width="860px">
      <el-form :inline="true" class="lineup-top">
        <el-form-item label="已通过赛事">
          <el-select v-model="lineupEventId" placeholder="请选择赛事" style="width:380px" @change="loadLineupData">
            <el-option v-for="event in approvedJoinedEvents" :key="event.id" :label="`${event.name || ('赛事#' + event.id)} | ${event.time || '-'} | ${event.location || '-'}`" :value="event.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table :data="lineupMembers" border size="small" v-loading="lineupLoading">
        <el-table-column prop="name" label="队员" min-width="140" />
        <el-table-column prop="role" label="角色" width="120" />
        <el-table-column label="是否参赛" width="120">
          <template slot-scope="scope">
            <el-switch v-model="scope.row.selected" />
          </template>
        </el-table-column>
        <el-table-column label="位置" width="180">
          <template slot-scope="scope">
            <el-select v-model="scope.row.position" :disabled="!scope.row.selected" placeholder="位置" size="mini" style="width:140px">
              <el-option v-for="item in positionOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
      <div class="empty-tip" v-if="!lineupLoading && lineupMembers.length === 0">当前球队暂无可安排队员</div>
      <div slot="footer">
        <el-button @click="lineupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingLineup" @click="saveLineup">保存安排并通知球员</el-button>
      </div>
    </el-dialog>

    <el-dialog title="入队申请管理" :visible.sync="applicationDialogVisible" width="880px">
      <el-table :data="teamApplications" border size="small" v-loading="applicationLoading">
        <el-table-column prop="id" label="申请ID" width="90" />
        <el-table-column prop="userId" label="申请人ID" width="110" />
        <el-table-column prop="requestPosition" label="申请位置" width="120" />
        <el-table-column prop="requestReason" label="申请理由" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column prop="status" label="状态" width="110">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 'pending' ? 'warning' : scope.row.status === 'approved' ? 'success' : 'danger'">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" :disabled="scope.row.status !== 'pending'" @click="handleApplication(scope.row, 'approved')">同意</el-button>
            <el-button size="mini" type="danger" :disabled="scope.row.status !== 'pending'" @click="handleApplication(scope.row, 'rejected')">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="empty-tip" v-if="!applicationLoading && teamApplications.length === 0">当前球队暂无待处理申请</div>
    </el-dialog>
  </div>
</template>

<script>
import * as teamApi from '@/api/team'
import * as applicationApi from '@/api/application'
import * as playerApi from '@/api/player'

export default {
  name: 'FrontTeamManagement',
  data() {
    return {
      activeMenu: 'team-management',
      teams: [],
      activeTeamId: null,
      pendingCreateApplication: false,
      pendingJoinApplication: false,
      teamDialogVisible: false,
      applyDialogVisible: false,
      joinDialogVisible: false,
      matchApplyDialogVisible: false,
      lineupDialogVisible: false,
      applicationDialogVisible: false,
      submittingTeam: false,
      submittingApply: false,
      submittingJoin: false,
      submittingMatchApply: false,
      lineupLoading: false,
      savingLineup: false,
      memberTableLoading: false,
      memberTableData: [],
      applicationLoading: false,
      teamApplications: [],
      joinTeams: [],
      matchEvents: [],
      approvedJoinedEvents: [],
      lineupEventId: null,
      lineupMembers: [],
      positionOptions: ['守门员', '后卫', '中场', '前锋', '待定'],
      coverMap: {},
      teamForm: { id: null, name: '', city: '', coach: '', description: '' },
      applyForm: { name: '', city: '', coach: '', description: '' },
      joinForm: { teamId: null, position: '', reason: '' },
      matchApplyForm: { eventId: null, reason: '' },
      teamRules: {
        name: [{ required: true, message: '请输入球队名称', trigger: 'blur' }],
        city: [{ required: true, message: '请输入所在城市', trigger: 'blur' }],
        coach: [{ required: true, message: '请输入主教练', trigger: 'blur' }]
      },
      joinRules: {
        teamId: [{ required: true, message: '请选择目标球队', trigger: 'change' }],
        position: [{ required: true, message: '请选择申请位置', trigger: 'change' }]
      },
      matchApplyRules: {
        eventId: [{ required: true, message: '请选择赛事', trigger: 'change' }]
      }
    }
  },
  computed: {
    user() {
      return this.$store.state.user
    },
    activeTeam() {
      return this.teams.find(item => item.id === this.activeTeamId) || null
    },
    activeTeamCover() {
      if (!this.activeTeam) return this.defaultCover('TEAM')
      return this.coverMap[this.activeTeam.id] || this.defaultCover(this.activeTeam.name || 'TEAM')
    }
  },
  mounted() {
    this.loadCoverMap()
    this.refreshAll()
  },
  methods: {
    handleMenuSelect(key) {
      this.$router.push(`/front/${key}`)
    },
    async refreshAll() {
      await this.loadTeamData()
      await this.loadTeamMembers()
    },
    async loadTeamData() {
      this.teams = []
      this.activeTeamId = null
      this.pendingCreateApplication = false
      this.pendingJoinApplication = false
      if (!this.user || !this.user.id) return
      try {
        const list = await teamApi.getUserTeams(this.user.id)
        this.teams = Array.isArray(list) ? list : []
        if (this.teams.length > 0) this.activeTeamId = this.teams[0].id
      } catch (e) {
        this.$message.error('加载球队信息失败')
      }
      try {
        const applications = await applicationApi.getUserApplications(this.user.id)
        const appList = Array.isArray(applications) ? applications : []
        this.pendingCreateApplication = appList.some(app => app.type === 'create_team' && app.status === 'pending')
        this.pendingJoinApplication = appList.some(app => app.type === 'team' && app.status === 'pending')
      } catch (e) {
        this.pendingCreateApplication = false
        this.pendingJoinApplication = false
      }
    },
    async loadTeamMembers() {
      this.memberTableLoading = true
      try {
        if (!this.activeTeam) {
          this.memberTableData = []
          return
        }
        const list = await playerApi.getPlayersByTeam(this.activeTeamId)
        const members = Array.isArray(list) ? list : []
        if (members.length > 0) {
          this.memberTableData = members.map(item => ({
            name: item.name || '-',
            position: item.position || '待定',
            age: item.age == null ? '-' : item.age,
            jerseyNumber: item.jerseyNumber == null ? '-' : item.jerseyNumber,
            remark: '队员'
          }))
        } else {
          this.memberTableData = [{
            name: this.activeTeam.coach || '队长',
            position: '队长',
            age: '-',
            jerseyNumber: '-',
            remark: '当前暂无队员，默认显示队长'
          }]
        }
      } catch (e) {
        this.memberTableData = []
        this.$message.error('加载队员信息失败')
      } finally {
        this.memberTableLoading = false
      }
    },
    openEditDialog(team) {
      this.teamForm = { id: team.id, name: team.name || '', city: team.city || '', coach: team.coach || '', description: team.description || '' }
      this.teamDialogVisible = true
      this.$nextTick(() => this.$refs.teamFormRef && this.$refs.teamFormRef.clearValidate())
    },
    submitTeam() {
      this.$refs.teamFormRef.validate(async valid => {
        if (!valid || !this.user || !this.user.id) return
        this.submittingTeam = true
        try {
          await teamApi.updateTeam({ ...this.teamForm, userId: this.user.id, status: 1, deleted: 0 })
          this.$message.success('球队信息已更新')
          this.teamDialogVisible = false
          await this.refreshAll()
        } catch (e) {
          this.$message.error(e.message || '保存失败')
        } finally {
          this.submittingTeam = false
        }
      })
    },
    deleteCurrentTeam(team) {
      this.$confirm(`确定解散球队【${team.name}】吗？`, '提示', {
        confirmButtonText: '解散',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await teamApi.dissolveTeam(team.id)
          if (this.coverMap[team.id]) {
            this.$delete(this.coverMap, team.id)
            this.saveCoverMap()
          }
          this.$message.success('球队已解散，队员已收到通知')
          await this.refreshAll()
        } catch (e) {
          this.$message.error(e.message || '解散失败')
        }
      }).catch(() => {})
    },
    submitCreateApplication() {
      this.$refs.applyFormRef.validate(async valid => {
        if (!valid || !this.user || !this.user.id) return
        this.submittingApply = true
        try {
          await applicationApi.createTeamCreateApplication(this.user.id, this.applyForm)
          this.$message.success('创建球队申请已提交')
          this.applyDialogVisible = false
          this.applyForm = { name: '', city: '', coach: '', description: '' }
          await this.refreshAll()
        } catch (e) {
          this.$message.error(e.message || '提交申请失败')
        } finally {
          this.submittingApply = false
        }
      })
    },
    async openJoinDialog() {
      if (!this.user || !this.user.id) return
      this.joinForm = { teamId: null, position: '', reason: '' }
      await this.loadJoinTeams()
      if (this.joinTeams.length === 0) {
        this.$message.warning('当前没有可申请的球队')
        return
      }
      this.joinDialogVisible = true
      this.$nextTick(() => this.$refs.joinFormRef && this.$refs.joinFormRef.clearValidate())
    },
    async loadJoinTeams() {
      try {
        const allTeams = await teamApi.getAllTeams()
        this.joinTeams = Array.isArray(allTeams) ? allTeams : []
      } catch (e) {
        this.joinTeams = []
      }
    },
    submitJoinApplication() {
      this.$refs.joinFormRef.validate(async valid => {
        if (!valid || !this.user || !this.user.id) return
        this.submittingJoin = true
        try {
          const payload = `position=${(this.joinForm.position || '').trim()};reason=${(this.joinForm.reason || '').trim()}`
          await applicationApi.createTeamApplication(this.user.id, this.joinForm.teamId, payload)
          this.$message.success('入队申请已提交')
          this.joinDialogVisible = false
          await this.refreshAll()
        } catch (e) {
          this.$message.error(e.message || '提交入队申请失败')
        } finally {
          this.submittingJoin = false
        }
      })
    },
    async openMatchApplyDialog() {
      if (!this.activeTeam) return
      this.matchApplyForm = { eventId: null, reason: '' }
      try {
        const allApps = await applicationApi.getAllApplications()
        this.matchEvents = (Array.isArray(allApps) ? allApps : []).filter(item => item.type === 'create' && item.status === 'approved')
      } catch (e) {
        this.matchEvents = []
      }
      if (this.matchEvents.length === 0) {
        this.$message.warning('当前没有可报名赛事')
        return
      }
      this.matchApplyDialogVisible = true
      this.$nextTick(() => this.$refs.matchApplyFormRef && this.$refs.matchApplyFormRef.clearValidate())
    },
    submitMatchApplication() {
      this.$refs.matchApplyFormRef.validate(async valid => {
        if (!valid || !this.user || !this.user.id || !this.activeTeam) return
        this.submittingMatchApply = true
        try {
          const reason = [
            `teamId=${this.activeTeam.id}`,
            `teamName=${this.activeTeam.name || ''}`,
            `reason=${this.matchApplyForm.reason || ''}`
          ].join(';')
          await applicationApi.createMatchApplication(this.user.id, this.matchApplyForm.eventId, reason)
          this.$message.success('战队报名已提交，等待赛事管理员审核')
          this.matchApplyDialogVisible = false
        } catch (e) {
          this.$message.error(e.message || '提交战队报名失败')
        } finally {
          this.submittingMatchApply = false
        }
      })
    },
    async openLineupDialog() {
      if (!this.activeTeam) return
      this.lineupDialogVisible = true
      this.lineupEventId = null
      this.lineupMembers = []
      try {
        const allApps = await applicationApi.getAllApplications()
        this.approvedJoinedEvents = (Array.isArray(allApps) ? allApps : []).filter(item => {
          if (item.type !== 'match' || item.status !== 'approved') return false
          const meta = this.parseMeta(item.reason)
          return Number(meta.teamId) === Number(this.activeTeamId)
        }).map(item => {
          const event = (Array.isArray(allApps) ? allApps : []).find(e => Number(e.id) === Number(item.targetId) && e.type === 'create')
          return event || null
        }).filter(Boolean)
      } catch (e) {
        this.approvedJoinedEvents = []
      }
      if (this.approvedJoinedEvents.length === 0) {
        this.$message.warning('当前球队暂无已通过审核的赛事，无法安排')
      } else {
        this.lineupEventId = this.approvedJoinedEvents[0].id
        await this.loadLineupData()
      }
    },
    async loadLineupData() {
      if (!this.lineupEventId || !this.activeTeamId) {
        this.lineupMembers = []
        return
      }
      this.lineupLoading = true
      try {
        let members = []
        let lineup = []
        try {
          members = await applicationApi.getTeamMembersForLineup(this.lineupEventId, this.activeTeamId)
          lineup = await applicationApi.getTeamLineup(this.lineupEventId, this.activeTeamId)
        } catch (e) {
          const allApps = await applicationApi.getAllApplications()
          const match = (Array.isArray(allApps) ? allApps : []).find(item =>
            item.type === 'match' &&
            item.status === 'approved' &&
            Number(item.targetId) === Number(this.lineupEventId) &&
            Number(this.parseMeta(item.reason).teamId) === Number(this.activeTeamId)
          )
          members = match ? [{ userId: match.userId, name: `用户#${match.userId}`, role: 'captain' }] : []
          lineup = []
        }

        const lineupMap = new Map((Array.isArray(lineup) ? lineup : []).map(item => [Number(item.userId), item]))
        this.lineupMembers = (Array.isArray(members) ? members : []).map(item => {
          const current = lineupMap.get(Number(item.userId))
          return {
            userId: item.userId,
            name: item.name || `用户#${item.userId}`,
            role: item.role || 'member',
            selected: !!current,
            position: current ? (current.position || '待定') : '待定'
          }
        })
      } finally {
        this.lineupLoading = false
      }
    },
    async saveLineup() {
      if (!this.lineupEventId || !this.activeTeamId) {
        this.$message.warning('请先选择赛事')
        return
      }
      const selected = this.lineupMembers.filter(item => item.selected).map(item => ({
        userId: item.userId,
        position: item.position || '待定'
      }))
      this.savingLineup = true
      try {
        await applicationApi.saveTeamLineup(this.lineupEventId, this.activeTeamId, {
          arrangerUserId: this.user && this.user.id,
          lineup: selected
        })
        this.$message.success('球队安排已保存，通知已发送给球员')
      } catch (e) {
        this.$message.error(e.message || '保存球队安排失败')
      } finally {
        this.savingLineup = false
      }
    },
    async openApplicationDialog() {
      if (!this.activeTeam) return
      this.applicationDialogVisible = true
      await this.loadTeamApplications()
    },
    async loadTeamApplications() {
      this.applicationLoading = true
      try {
        const all = await applicationApi.getAllApplications()
        this.teamApplications = (Array.isArray(all) ? all : [])
          .filter(item => item.type === 'team' && Number(item.targetId) === Number(this.activeTeamId))
          .map(item => {
            const meta = this.parseMeta(item.reason)
            return {
              ...item,
              requestPosition: meta.position || '待定',
              requestReason: meta.reason || '-'
            }
          })
      } catch (e) {
        this.teamApplications = []
      } finally {
        this.applicationLoading = false
      }
    },
    parseMeta(raw) {
      const result = {}
      if (!raw) return result
      String(raw).split(';').forEach(part => {
        const idx = part.indexOf('=')
        if (idx > 0) {
          result[part.slice(0, idx).trim()] = part.slice(idx + 1).trim()
        }
      })
      return result
    },
    async handleApplication(row, status) {
      try {
        await applicationApi.updateApplicationStatus(row.id, status)
        await this.loadTeamApplications()
      } catch (e) {
        this.$message.error(e.message || '处理失败')
      }
    },
    defaultCover(teamName) {
      return `https://placehold.co/1000x420/edf2f7/64748b?text=${encodeURIComponent(teamName || 'TEAM')}`
    },
    loadCoverMap() {
      try {
        this.coverMap = JSON.parse(localStorage.getItem('team_cover_map') || '{}')
      } catch (e) {
        this.coverMap = {}
      }
    },
    saveCoverMap() {
      localStorage.setItem('team_cover_map', JSON.stringify(this.coverMap))
    },
    triggerUpload() {
      if (!this.activeTeam) return
      this.$refs.coverInput && this.$refs.coverInput.click()
    },
    handleCoverUpload(e) {
      const file = e.target.files && e.target.files[0]
      if (!file || !this.activeTeam) return
      const reader = new FileReader()
      reader.onload = () => {
        this.$set(this.coverMap, this.activeTeam.id, reader.result)
        this.saveCoverMap()
        this.$message.success('球队封面上传成功')
      }
      reader.readAsDataURL(file)
      e.target.value = ''
    },
    async handleLogout() {
      await this.$store.dispatch('logout')
      this.$router.push('/front/home')
    }
  }
}
</script>

<style scoped>
.team-management { min-height: 100vh; background: #f5f7fa; }
.top-nav { position: sticky; top: 0; z-index: 1000; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.header-content { max-width: 1200px; margin: 0 auto; padding: 0 20px; height: 60px; display: flex; align-items: center; }
.logo { display: flex; align-items: center; font-weight: 700; }
.logo i { color: #409eff; margin-right: 8px; }
.nav-menu { flex: 1; margin: 0 24px; }
.user-info { display: flex; align-items: center; gap: 8px; }
.main-content { max-width: 1160px; margin: 24px auto; padding: 0 16px 20px; }
.main-card, .team-list-card, .team-detail-card, .all-team-card { border-radius: 12px; }
.hint { color: #606266; }
.actions { display: flex; gap: 10px; margin-top: 8px; }
.manage-layout { display: grid; grid-template-columns: 280px 1fr; gap: 16px; margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.cover-box { margin-top: 12px; width: 100%; height: 320px; border: 1px solid #ebeef5; border-radius: 8px; overflow: hidden; background: #f8fafc; }
.cover-box img { width: 100%; height: 100%; object-fit: cover; display: block; }
.sub-text { color: #909399; font-size: 12px; }
.lineup-top { margin-bottom: 10px; }
.empty-tip { text-align: center; color: #909399; padding-top: 8px; }
</style>
