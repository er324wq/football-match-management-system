<template>
  <div class="match-application-management">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>赛事申请管理</span>
          <el-button type="primary" size="mini" @click="loadEventList">刷新</el-button>
        </div>
      </template>

      <div class="search-form">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="赛事名称">
            <el-input v-model="searchForm.name" placeholder="请输入赛事名称" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
              <el-option label="全部" value="" />
              <el-option label="待审核" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已拒绝" value="rejected" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="pagedEvents" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="赛事名称" min-width="160" />
        <el-table-column prop="time" label="计划时间" width="180" />
        <el-table-column prop="location" label="地点" min-width="120" />
        <el-table-column prop="matchType" label="类型" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 'pending' ? 'info' : scope.row.status === 'approved' ? 'success' : 'danger'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template slot-scope="scope">
            <el-button type="primary" size="mini" @click="openEventDialog(scope.row)">查看</el-button>
            <el-button
              v-if="scope.row.status === 'pending'"
              type="success"
              size="mini"
              @click="handleEventStatus(scope.row.id, 'approved')"
            >
              同意
            </el-button>
            <el-button
              v-if="scope.row.status === 'pending'"
              type="danger"
              size="mini"
              @click="handleEventStatus(scope.row.id, 'rejected')"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredEvents.length"
        />
      </div>
    </el-card>

    <el-dialog title="赛事详情与参赛管理" :visible.sync="viewDialogVisible" width="1100px">
      <el-descriptions :column="2" border v-if="viewForm.id">
        <el-descriptions-item label="赛事ID">{{ viewForm.id }}</el-descriptions-item>
        <el-descriptions-item label="赛事名称">{{ viewForm.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划时间">{{ viewForm.time || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地点">{{ viewForm.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ viewForm.matchType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ viewForm.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ viewForm.description || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="section-title">参赛战队申请</div>
      <el-table :data="eventTeamApplications" border size="small" v-loading="eventTeamsLoading">
        <el-table-column prop="id" label="申请ID" width="90" />
        <el-table-column prop="teamName" label="战队" min-width="140" />
        <el-table-column prop="userName" label="申请人" min-width="120" />
        <el-table-column prop="position" label="位置" width="120" />
        <el-table-column prop="reason" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 'pending' ? 'warning' : scope.row.status === 'approved' ? 'success' : 'danger'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="success"
              :disabled="scope.row.status !== 'pending'"
              @click="handleTeamApplyStatus(scope.row.id, 'approved')"
            >
              同意
            </el-button>
            <el-button
              size="mini"
              type="danger"
              :disabled="scope.row.status !== 'pending'"
              @click="handleTeamApplyStatus(scope.row.id, 'rejected')"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="section-title">已参赛球队管理</div>
      <el-table :data="approvedTeams" border size="small">
        <el-table-column prop="teamName" label="战队" min-width="160" />
        <el-table-column prop="userName" label="负责人" min-width="140" />
        <el-table-column prop="position" label="位置" width="120" />
        <el-table-column prop="reason" label="备注" min-width="180" show-overflow-tooltip />
      </el-table>

      <div class="section-title with-action">
        <span>已安排比赛</span>
        <el-button
          type="primary"
          size="mini"
          :disabled="!viewForm.id || viewForm.status !== 'approved'"
          :loading="arranging"
          @click="handleRandomArrange"
        >
          随机安排比赛
        </el-button>
      </div>
      <el-table :data="eventMatches" border size="small" v-loading="eventMatchesLoading">
        <el-table-column prop="id" label="比赛ID" width="90" />
        <el-table-column prop="homeTeamName" label="主队" min-width="140" />
        <el-table-column prop="awayTeamName" label="客队" min-width="140" />
        <el-table-column prop="matchTime" label="比赛时间" min-width="180" />
        <el-table-column prop="stadium" label="球场" min-width="140" />
        <el-table-column prop="status" label="状态" width="100" />
      </el-table>

      <span slot="footer">
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import * as applicationApi from '@/api/application'
import * as matchApi from '@/api/match'

export default {
  name: 'MatchApplicationManagement',
  data() {
    return {
      allEvents: [],
      filteredEvents: [],
      pagedEvents: [],
      searchForm: { name: '', status: '' },
      currentPage: 1,
      pageSize: 10,

      viewDialogVisible: false,
      viewForm: {},
      eventTeamApplications: [],
      approvedTeams: [],
      eventMatches: [],
      eventTeamsLoading: false,
      eventMatchesLoading: false,
      arranging: false
    }
  },
  mounted() {
    this.loadEventList()
  },
  methods: {
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
    formatDateTime(d) {
      const date = d instanceof Date ? d : new Date(d)
      const pad = n => String(n).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    },
    async loadEventList() {
      try {
        const apps = await applicationApi.getAllApplications()
        this.allEvents = (Array.isArray(apps) ? apps : [])
          .filter(item => item.type === 'create')
          .sort((a, b) => (b.id || 0) - (a.id || 0))
        this.applyFilters()
      } catch (e) {
        this.allEvents = []
        this.filteredEvents = []
        this.pagedEvents = []
        this.$message.error('获取赛事申请失败')
      }
    },
    applyFilters() {
      const keyword = (this.searchForm.name || '').trim().toLowerCase()
      const status = this.searchForm.status
      this.filteredEvents = this.allEvents.filter(item => {
        const okName = !keyword || (item.name || '').toLowerCase().includes(keyword)
        const okStatus = !status || item.status === status
        return okName && okStatus
      })
      const start = (this.currentPage - 1) * this.pageSize
      this.pagedEvents = this.filteredEvents.slice(start, start + this.pageSize)
    },
    handleSearch() {
      this.currentPage = 1
      this.applyFilters()
    },
    resetForm() {
      this.searchForm = { name: '', status: '' }
      this.currentPage = 1
      this.applyFilters()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.applyFilters()
    },
    handleCurrentChange(page) {
      this.currentPage = page
      this.applyFilters()
    },
    async handleEventStatus(id, status) {
      try {
        await applicationApi.updateApplicationStatus(id, status)
        this.$message.success(status === 'approved' ? '赛事申请已同意' : '赛事申请已拒绝')
        await this.loadEventList()
      } catch (e) {
        this.$message.error('处理失败')
      }
    },
    async openEventDialog(row) {
      this.viewForm = { ...row }
      this.viewDialogVisible = true
      await this.reloadEventDetailData()
    },
    async reloadEventDetailData() {
      if (!this.viewForm.id) return
      this.eventTeamsLoading = true
      this.eventMatchesLoading = true
      try {
        const allApps = await applicationApi.getAllApplications()
        const appList = Array.isArray(allApps) ? allApps : []

        this.eventTeamApplications = appList
          .filter(item => item.type === 'match' && Number(item.targetId) === Number(this.viewForm.id))
          .map(item => {
            const meta = this.parseMeta(item.reason)
            return {
              id: item.id,
              userId: item.userId,
              userName: `用户#${item.userId || '-'}`,
              teamId: Number(meta.teamId || 0),
              teamName: meta.teamName || '-',
              position: meta.position || '待定',
              reason: meta.reason || '-',
              status: item.status
            }
          })

        this.approvedTeams = this.eventTeamApplications.filter(item => item.status === 'approved')

        const mappings = appList
          .filter(item => item.type === 'event_match' && Number(item.targetId) === Number(this.viewForm.id) && item.status === 'approved')
          .map(item => Number(item.reason))
          .filter(id => id > 0)

        const allMatches = await matchApi.getAllMatches()
        const matchList = Array.isArray(allMatches) ? allMatches : []

        if (mappings.length > 0) {
          this.eventMatches = matchList.filter(item => mappings.includes(Number(item.id)))
        } else {
          const approvedTeamIds = this.approvedTeams.map(item => Number(item.teamId)).filter(id => id > 0)
          this.eventMatches = matchList.filter(item =>
            approvedTeamIds.includes(Number(item.homeTeamId)) &&
            approvedTeamIds.includes(Number(item.awayTeamId)) &&
            (!this.viewForm.location || item.stadium === this.viewForm.location)
          )
        }
      } catch (e) {
        this.eventTeamApplications = []
        this.approvedTeams = []
        this.eventMatches = []
        this.$message.error('加载赛事详情失败')
      } finally {
        this.eventTeamsLoading = false
        this.eventMatchesLoading = false
      }
    },
    async handleTeamApplyStatus(id, status) {
      try {
        await applicationApi.updateApplicationStatus(id, status)
        this.$message.success(status === 'approved' ? '参赛申请已同意' : '参赛申请已拒绝')
        await this.reloadEventDetailData()
      } catch (e) {
        this.$message.error('处理参赛申请失败')
      }
    },
    async handleRandomArrange() {
      if (!this.viewForm.id) return
      this.arranging = true
      try {
        const count = await applicationApi.randomArrangeMatches(this.viewForm.id)
        this.$message.success(`随机安排完成，已生成 ${count || 0} 场比赛`)
      } catch (e) {
        const approvedTeamIds = this.approvedTeams.map(item => Number(item.teamId)).filter(id => id > 0)
        if (approvedTeamIds.length < 2) {
          this.$message.error(e.message || '随机安排失败，至少需要2支已参赛球队')
          this.arranging = false
          return
        }
        const shuffled = approvedTeamIds.slice().sort(() => Math.random() - 0.5)
        const baseTime = this.viewForm.time ? new Date(this.viewForm.time.replace(/-/g, '/')) : new Date()
        let created = 0
        for (let i = 0; i + 1 < shuffled.length; i += 2) {
          const matchTime = new Date(baseTime.getTime() + created * 2 * 60 * 60 * 1000)
          await matchApi.addMatch({
            homeTeamId: shuffled[i],
            awayTeamId: shuffled[i + 1],
            matchTime: this.formatDateTime(matchTime),
            stadium: this.viewForm.location || '待定球场',
            status: 'upcoming'
          })
          created++
        }
        this.$message.success(`随机安排完成（兼容模式），已生成 ${created} 场比赛`)
      } finally {
        this.arranging = false
        await this.reloadEventDetailData()
      }
    }
  }
}
</script>

<style scoped>
.match-application-management {
  padding: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
.section-title {
  margin: 16px 0 10px;
  font-weight: 700;
  color: #303133;
}
.section-title.with-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
