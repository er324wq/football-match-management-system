<template>
  <div class="front-match">
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
          <span v-if="user">{{ user.nickname || user.username }} ({{ roleText }})</span>
          <span v-else>游客</span>
          <el-button type="text" v-if="user" @click="handleLogout">退出</el-button>
          <el-button type="text" v-else @click="$router.push('/login')">登录</el-button>
        </div>
      </div>
    </header>

    <main class="match-content">
      <el-card class="featured-card">
        <div class="section-head">
          <h3>精选赛事</h3>
          <span class="muted">从左到右：已结束、进行中、未开始</span>
        </div>
        <div class="featured-grid">
          <div v-for="item in featuredEvents" :key="item.id" class="featured-item">
            <div class="featured-title">{{ item.title }}</div>
            <el-tag :type="statusTagType(item.status)" size="mini">{{ statusText(item.status) }}</el-tag>
            <div class="featured-match">{{ item.highlightText }}</div>
            <div class="featured-meta">共 {{ item.count }} 场比赛</div>
            <el-button size="mini" type="primary" @click="openFeatured(item)">打开赛事</el-button>
          </div>
        </div>
      </el-card>

      <el-card>
        <div class="toolbar">
          <el-form :inline="true" :model="filterForm">
            <el-form-item label="状态">
              <el-select v-model="filterForm.status" placeholder="全部状态">
                <el-option label="全部" value="" />
                <el-option label="未开始" value="upcoming" />
                <el-option label="进行中" value="in_progress" />
                <el-option label="已结束" value="finished" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadMatches">查询</el-button>
              <el-button @click="resetFilter">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <el-table :data="matches" border v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="比赛" min-width="220">
            <template slot-scope="scope">{{ scope.row.homeTeamName }} vs {{ scope.row.awayTeamName }}</template>
          </el-table-column>
          <el-table-column prop="matchTime" label="时间" min-width="170" />
          <el-table-column prop="stadium" label="地点" min-width="140" />
          <el-table-column label="状态" width="110">
            <template slot-scope="scope">
              <el-tag :type="statusTagType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" @click="viewMatch(scope.row)">详情</el-button>
              <el-button v-if="canReferee(scope.row)" size="mini" type="danger" @click="openRefereeDialog(scope.row)">裁判执裁</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </main>

    <el-dialog title="比赛详情" :visible.sync="detailVisible" width="560px">
      <el-descriptions :column="1" border v-if="detailMatch">
        <el-descriptions-item label="比赛ID">{{ detailMatch.id }}</el-descriptions-item>
        <el-descriptions-item label="对阵">{{ detailMatch.homeTeamName }} vs {{ detailMatch.awayTeamName }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detailMatch.matchTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地点">{{ detailMatch.stadium || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detailMatch.status) }}</el-descriptions-item>
      </el-descriptions>
      <span slot="footer"><el-button @click="detailVisible = false">关闭</el-button></span>
    </el-dialog>

    <el-dialog :title="featuredDialogTitle" :visible.sync="featuredDialogVisible" width="980px">
      <el-alert type="info" :closable="false" title="排序规则：未开始在最上方，进行中居中，已结束在最下方" style="margin-bottom: 12px" />
      <el-table :data="featuredMatches" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="比赛" min-width="220">
          <template slot-scope="scope">{{ scope.row.homeTeamName }} vs {{ scope.row.awayTeamName }}</template>
        </el-table-column>
        <el-table-column prop="matchTime" label="时间" min-width="170" />
        <el-table-column prop="stadium" label="地点" min-width="140" />
        <el-table-column label="状态" width="110">
          <template slot-scope="scope"><el-tag :type="statusTagType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag></template>
        </el-table-column>
      </el-table>
      <span slot="footer"><el-button @click="featuredDialogVisible = false">关闭</el-button></span>
    </el-dialog>

    <el-dialog title="裁判执裁记录" :visible.sync="refereeVisible" width="980px">
      <el-alert
        v-if="refereeMatch"
        type="info"
        :closable="false"
        :title="`当前比赛：${refereeMatch.homeTeamName} vs ${refereeMatch.awayTeamName}`"
        style="margin-bottom: 12px"
      />

      <el-tabs v-model="refereeTab">
        <el-tab-pane label="比赛结果" name="result">
          <el-form :inline="true">
            <el-form-item label="主队比分"><el-input-number v-model="refereeResult.homeScore" :min="0" /></el-form-item>
            <el-form-item label="客队比分"><el-input-number v-model="refereeResult.awayScore" :min="0" /></el-form-item>
            <el-form-item label="裁判"><el-input v-model="refereeResult.referee" placeholder="默认当前裁判账号" /></el-form-item>
            <el-form-item><el-button type="primary" :loading="savingResult" @click="submitMatchResult">提交比赛结果并结束比赛</el-button></el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="进球记录" name="goal">
          <el-form :inline="true">
            <el-form-item label="球员">
              <el-select v-model="goalForm.playerId" placeholder="请选择球员" filterable style="width: 200px">
                <el-option v-for="p in refereePlayers" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="进球时间(分钟)"><el-input-number v-model="goalForm.goalTime" :min="1" :max="130" /></el-form-item>
            <el-form-item><el-button type="success" :loading="savingGoal" @click="submitGoalRecord">记录进球</el-button></el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="红黄牌" name="card">
          <el-form :inline="true">
            <el-form-item label="球员">
              <el-select v-model="cardForm.playerId" placeholder="请选择球员" filterable style="width: 200px">
                <el-option v-for="p in refereePlayers" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="cardForm.cardType" style="width: 110px">
                <el-option label="黄牌" value="yellow" />
                <el-option label="红牌" value="red" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间(分钟)"><el-input-number v-model="cardForm.cardTime" :min="1" :max="130" /></el-form-item>
            <el-form-item label="原因"><el-input v-model="cardForm.reason" placeholder="可选" style="width: 220px" /></el-form-item>
            <el-form-item><el-button type="warning" :loading="savingCard" @click="submitCardRecord">记录红黄牌</el-button></el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <span slot="footer"><el-button @click="refereeVisible = false">关闭</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import * as matchApi from '@/api/match'
import * as playerApi from '@/api/player'
import * as goalRecordApi from '@/api/goalRecord'
import * as cardRecordApi from '@/api/cardRecord'
import * as matchResultApi from '@/api/matchResult'

export default {
  name: 'FrontMatch',
  data() {
    return {
      activeMenu: 'match',
      loading: false,
      filterForm: { status: '' },
      matches: [],

      featuredEvents: [],
      featuredDialogVisible: false,
      featuredDialogTitle: '',
      featuredMatches: [],

      detailVisible: false,
      detailMatch: null,

      refereeVisible: false,
      refereeTab: 'result',
      refereeMatch: null,
      refereePlayers: [],
      refereeResult: { homeScore: 0, awayScore: 0, referee: '' },
      goalForm: { playerId: null, goalTime: 1 },
      cardForm: { playerId: null, cardType: 'yellow', cardTime: 1, reason: '' },
      savingResult: false,
      savingGoal: false,
      savingCard: false
    }
  },
  computed: {
    user() {
      return this.$store.state.user
    },
    roleText() {
      if (!this.user) return '游客'
      if (this.user.role === 'admin') return '管理员'
      if (this.user.role === 'referee') return '裁判'
      return '用户'
    }
  },
  mounted() {
    this.loadMatches()
  },
  methods: {
    handleSelect(key) {
      if (key === 'team-management') {
        this.$router.push('/front/team-management')
      } else {
        this.$router.push(`/front/${key}`)
      }
    },
    normalizeStatus(status) {
      if (status === 'ongoing') return 'in_progress'
      return status || ''
    },
    statusText(status) {
      const s = this.normalizeStatus(status)
      if (s === 'upcoming') return '未开始'
      if (s === 'in_progress') return '进行中'
      if (s === 'finished') return '已结束'
      return s || '-'
    },
    statusTagType(status) {
      const s = this.normalizeStatus(status)
      if (s === 'upcoming') return 'info'
      if (s === 'in_progress') return 'warning'
      if (s === 'finished') return 'success'
      return ''
    },
    statusOrder(status) {
      const s = this.normalizeStatus(status)
      if (s === 'upcoming') return 0
      if (s === 'in_progress') return 1
      if (s === 'finished') return 2
      return 1
    },
    sortMatchesForFeatured(list) {
      return (list || []).slice().sort((a, b) => {
        const orderDiff = this.statusOrder(a.status) - this.statusOrder(b.status)
        if (orderDiff !== 0) return orderDiff
        const ta = new Date((a.matchTime || '').replace(/-/g, '/')).getTime() || 0
        const tb = new Date((b.matchTime || '').replace(/-/g, '/')).getTime() || 0
        return ta - tb
      })
    },
    pickRepresentative(source, targetStatus) {
      const exact = source.find(item => this.normalizeStatus(item.status) === targetStatus)
      if (exact) return exact
      return source[0] || null
    },
    buildFeaturedEvents() {
      const all = this.sortMatchesForFeatured(this.matches)
      const buildPool = (seed) => {
        const subset = all.filter((item, idx) => ((Number(item.id || idx) + seed) % 3) === 0)
        if (subset.length >= 3) return subset
        return all.slice(0, Math.min(all.length, 8))
      }

      const finishedHighlight = this.pickRepresentative(all, 'finished')
      const progressHighlight = this.pickRepresentative(all, 'in_progress')
      const upcomingHighlight = this.pickRepresentative(all, 'upcoming')

      this.featuredEvents = [
        {
          id: 'featured-finished',
          title: '经典收官赛',
          status: 'finished',
          highlightText: finishedHighlight ? `${finishedHighlight.homeTeamName} vs ${finishedHighlight.awayTeamName}` : '暂无比赛',
          matches: this.sortMatchesForFeatured(buildPool(1)),
          count: buildPool(1).length
        },
        {
          id: 'featured-progress',
          title: '实时焦点赛',
          status: 'in_progress',
          highlightText: progressHighlight ? `${progressHighlight.homeTeamName} vs ${progressHighlight.awayTeamName}` : '暂无比赛',
          matches: this.sortMatchesForFeatured(buildPool(2)),
          count: buildPool(2).length
        },
        {
          id: 'featured-upcoming',
          title: '明日之星赛',
          status: 'upcoming',
          highlightText: upcomingHighlight ? `${upcomingHighlight.homeTeamName} vs ${upcomingHighlight.awayTeamName}` : '暂无比赛',
          matches: this.sortMatchesForFeatured(buildPool(3)),
          count: buildPool(3).length
        }
      ]
    },
    openFeatured(item) {
      this.featuredDialogTitle = `${item.title}（${this.statusText(item.status)}）`
      this.featuredMatches = this.sortMatchesForFeatured(item.matches)
      this.featuredDialogVisible = true
    },
    canReferee(match) {
      const s = this.normalizeStatus(match.status)
      return this.user && this.user.role === 'referee' && s === 'in_progress'
    },
    async loadMatches() {
      this.loading = true
      try {
        const res = await matchApi.getMatchList({ page: 1, size: 300, status: this.filterForm.status || '' })
        this.matches = (res && res.records) ? res.records : []
        this.buildFeaturedEvents()
      } catch (e) {
        this.matches = []
        this.featuredEvents = []
        this.$message.error('加载比赛失败')
      } finally {
        this.loading = false
      }
    },
    resetFilter() {
      this.filterForm = { status: '' }
      this.loadMatches()
    },
    viewMatch(row) {
      this.detailMatch = row
      this.detailVisible = true
    },
    async openRefereeDialog(row) {
      this.refereeMatch = row
      this.refereeVisible = true
      this.refereeTab = 'result'
      this.refereeResult = {
        homeScore: 0,
        awayScore: 0,
        referee: (this.user && (this.user.nickname || this.user.username)) || ''
      }
      this.goalForm = { playerId: null, goalTime: 1 }
      this.cardForm = { playerId: null, cardType: 'yellow', cardTime: 1, reason: '' }
      await this.loadRefereePlayers(row)
    },
    async loadRefereePlayers(match) {
      const home = await playerApi.getPlayersByTeam(match.homeTeamId)
      const away = await playerApi.getPlayersByTeam(match.awayTeamId)
      this.refereePlayers = [...(Array.isArray(home) ? home : []), ...(Array.isArray(away) ? away : [])]
    },
    async submitMatchResult() {
      if (!this.refereeMatch) return
      this.savingResult = true
      try {
        await matchApi.updateMatch({
          id: this.refereeMatch.id,
          status: 'finished',
          homeScore: this.refereeResult.homeScore,
          awayScore: this.refereeResult.awayScore,
          referee: this.refereeResult.referee
        })

        const resultList = await matchResultApi.getMatchResultList({ page: 1, size: 500 })
        const existed = (resultList.records || []).find(item => Number(item.matchId) === Number(this.refereeMatch.id))
        const payload = {
          id: existed ? existed.id : null,
          matchId: this.refereeMatch.id,
          homeScore: this.refereeResult.homeScore,
          awayScore: this.refereeResult.awayScore,
          referee: this.refereeResult.referee
        }
        if (payload.id) {
          await matchResultApi.updateMatchResult(payload)
        } else {
          await matchResultApi.addMatchResult(payload)
        }
        this.$message.success('比赛结果已提交，比赛已结束')
        await this.loadMatches()
      } catch (e) {
        this.$message.error(e.message || '提交比赛结果失败')
      } finally {
        this.savingResult = false
      }
    },
    async submitGoalRecord() {
      if (!this.refereeMatch || !this.goalForm.playerId) {
        this.$message.warning('请选择球员')
        return
      }
      this.savingGoal = true
      try {
        await goalRecordApi.createGoalRecord({
          matchId: this.refereeMatch.id,
          playerId: this.goalForm.playerId,
          goalTime: this.goalForm.goalTime
        })
        this.$message.success('进球记录已保存')
      } catch (e) {
        this.$message.error(e.message || '保存进球记录失败')
      } finally {
        this.savingGoal = false
      }
    },
    async submitCardRecord() {
      if (!this.refereeMatch || !this.cardForm.playerId) {
        this.$message.warning('请选择球员')
        return
      }
      this.savingCard = true
      try {
        await cardRecordApi.createCardRecord({
          matchId: this.refereeMatch.id,
          playerId: this.cardForm.playerId,
          cardType: this.cardForm.cardType,
          cardTime: this.cardForm.cardTime,
          reason: this.cardForm.reason
        })
        this.$message.success('红黄牌记录已保存')
      } catch (e) {
        this.$message.error(e.message || '保存红黄牌失败')
      } finally {
        this.savingCard = false
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
.front-match { min-height: 100vh; background: #f5f7fa; }
.top-nav { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.header-content { max-width: 1200px; margin: 0 auto; height: 60px; padding: 0 20px; display: flex; align-items: center; }
.logo { display: flex; align-items: center; font-weight: 700; }
.logo i { color: #409eff; margin-right: 8px; }
.nav-menu { flex: 1; margin: 0 24px; }
.user-info { display: flex; align-items: center; gap: 8px; }
.match-content { max-width: 1200px; margin: 18px auto; padding: 0 16px; }
.featured-card { margin-bottom: 16px; }
.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.muted { color: #909399; font-size: 12px; }
.featured-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.featured-item { background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 10px; padding: 12px; display: flex; flex-direction: column; gap: 8px; }
.featured-title { font-weight: 700; font-size: 16px; }
.featured-match { color: #303133; min-height: 20px; }
.featured-meta { font-size: 12px; color: #909399; }
.toolbar { margin-bottom: 12px; }
</style>
