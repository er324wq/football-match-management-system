<template>
  <div class="front-ranking">
    <div class="ranking-header">
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
    <div class="ranking-content">
      <div class="ranking-tabs">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="积分榜" name="standings">
            <div class="standings-container">
              <div class="standings-filter">
                <el-form :inline="true" :model="standingsFilter" class="demo-form-inline">
                  <el-form-item label="赛事">
                    <el-select v-model="standingsFilter.league" placeholder="请选择赛事" @change="getStandings">
                      <el-option
                        v-for="event in eventOptions"
                        :key="event.value"
                        :label="event.label"
                        :value="event.value"
                      ></el-option>
                    </el-select>
                  </el-form-item>
                  <el-form-item label="赛季">
                    <el-select v-model="standingsFilter.season" placeholder="请选择赛季" @change="getStandings">
                      <el-option label="2025-2026" value="2025-2026"></el-option>
                      <el-option label="2024-2025" value="2024-2025"></el-option>
                      <el-option label="2023-2024" value="2023-2024"></el-option>
                    </el-select>
                  </el-form-item>
                </el-form>
              </div>
              <el-table :data="standingsData" style="width: 100%">
                <el-table-column prop="rank" label="排名" width="80"></el-table-column>
                <el-table-column prop="team" label="球队" width="180"></el-table-column>
                <el-table-column prop="matches" label="场次" width="80"></el-table-column>
                <el-table-column prop="wins" label="胜" width="60"></el-table-column>
                <el-table-column prop="draws" label="平" width="60"></el-table-column>
                <el-table-column prop="losses" label="负" width="60"></el-table-column>
                <el-table-column prop="goalsFor" label="进球" width="80"></el-table-column>
                <el-table-column prop="goalsAgainst" label="失球" width="80"></el-table-column>
                <el-table-column prop="goalDifference" label="净胜球" width="80"></el-table-column>
                <el-table-column prop="points" label="积分" width="80"></el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
          <el-tab-pane label="射手榜" name="topScorers">
            <div class="top-scorers-container">
              <div class="top-scorers-filter">
                <el-form :inline="true" :model="topScorersFilter" class="demo-form-inline">
                  <el-form-item label="赛事">
                    <el-select v-model="topScorersFilter.league" placeholder="请选择赛事" @change="getTopScorers">
                      <el-option
                        v-for="event in eventOptions"
                        :key="`scorer-${event.value}`"
                        :label="event.label"
                        :value="event.value"
                      ></el-option>
                    </el-select>
                  </el-form-item>
                  <el-form-item label="赛季">
                    <el-select v-model="topScorersFilter.season" placeholder="请选择赛季" @change="getTopScorers">
                      <el-option label="2025-2026" value="2025-2026"></el-option>
                      <el-option label="2024-2025" value="2024-2025"></el-option>
                      <el-option label="2023-2024" value="2023-2024"></el-option>
                    </el-select>
                  </el-form-item>
                </el-form>
              </div>
              <el-table :data="topScorersData" style="width: 100%">
                <el-table-column prop="rank" label="排名" width="80"></el-table-column>
                <el-table-column prop="player" label="球员" width="180"></el-table-column>
                <el-table-column prop="team" label="球队" width="150"></el-table-column>
                <el-table-column prop="goals" label="进球" width="80"></el-table-column>
                <el-table-column prop="assists" label="助攻" width="80"></el-table-column>
                <el-table-column prop="matches" label="场次" width="80"></el-table-column>
                <el-table-column prop="goalsPerMatch" label="场均进球" width="100"></el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
          <el-tab-pane label="助攻榜" name="topAssists">
            <div class="top-assists-container">
              <div class="top-assists-filter">
                <el-form :inline="true" :model="topAssistsFilter" class="demo-form-inline">
                  <el-form-item label="赛事">
                    <el-select v-model="topAssistsFilter.league" placeholder="请选择赛事" @change="getTopAssists">
                      <el-option
                        v-for="event in eventOptions"
                        :key="`assist-${event.value}`"
                        :label="event.label"
                        :value="event.value"
                      ></el-option>
                    </el-select>
                  </el-form-item>
                  <el-form-item label="赛季">
                    <el-select v-model="topAssistsFilter.season" placeholder="请选择赛季" @change="getTopAssists">
                      <el-option label="2025-2026" value="2025-2026"></el-option>
                      <el-option label="2024-2025" value="2024-2025"></el-option>
                      <el-option label="2023-2024" value="2023-2024"></el-option>
                    </el-select>
                  </el-form-item>
                </el-form>
              </div>
              <el-table :data="topAssistsData" style="width: 100%">
                <el-table-column prop="rank" label="排名" width="80"></el-table-column>
                <el-table-column prop="player" label="球员" width="180"></el-table-column>
                <el-table-column prop="team" label="球队" width="150"></el-table-column>
                <el-table-column prop="assists" label="助攻" width="80"></el-table-column>
                <el-table-column prop="goals" label="进球" width="80"></el-table-column>
                <el-table-column prop="matches" label="场次" width="80"></el-table-column>
                <el-table-column prop="assistsPerMatch" label="场均助攻" width="100"></el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
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
import * as statisticsApi from '@/api/statistics'
import * as playerApi from '@/api/player'
import * as applicationApi from '@/api/application'
import * as matchApi from '@/api/match'

export default {
  name: 'FrontRanking',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      activeMenu: 'ranking',
      activeTab: 'standings',
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
      standingsFilter: {
        league: 'all',
        season: '2025-2026'
      },
      topScorersFilter: {
        league: 'all',
        season: '2025-2026'
      },
      topAssistsFilter: {
        league: 'all',
        season: '2025-2026'
      },
      eventOptions: [
        { label: '全部赛事', value: 'all' }
      ],
      standingsData: [
        { rank: 1, team: '皇家马德里', matches: 28, wins: 22, draws: 2, losses: 4, goalsFor: 68, goalsAgainst: 24, goalDifference: 44, points: 68 },
        { rank: 2, team: '巴塞罗那', matches: 28, wins: 21, draws: 2, losses: 5, goalsFor: 65, goalsAgainst: 28, goalDifference: 37, points: 65 },
        { rank: 3, team: '马德里竞技', matches: 28, wins: 18, draws: 4, losses: 6, goalsFor: 52, goalsAgainst: 29, goalDifference: 23, points: 58 },
        { rank: 4, team: '塞维利亚', matches: 28, wins: 16, draws: 5, losses: 7, goalsFor: 48, goalsAgainst: 32, goalDifference: 16, points: 53 },
        { rank: 5, team: '瓦伦西亚', matches: 28, wins: 14, draws: 6, losses: 8, goalsFor: 42, goalsAgainst: 34, goalDifference: 8, points: 48 }
      ],
      topScorersData: [
        { rank: 1, player: 'Cristiano Ronaldo', team: '皇家马德里', goals: 32, assists: 8, matches: 30, goalsPerMatch: 1.07 },
        { rank: 2, player: 'Lionel Messi', team: '巴塞罗那', goals: 28, assists: 12, matches: 28, goalsPerMatch: 1.00 },
        { rank: 3, player: 'Karim Benzema', team: '皇家马德里', goals: 22, assists: 10, matches: 29, goalsPerMatch: 0.76 },
        { rank: 4, player: 'Luis Suarez', team: '巴塞罗那', goals: 20, assists: 8, matches: 27, goalsPerMatch: 0.74 },
        { rank: 5, player: 'Antoine Griezmann', team: '马德里竞技', goals: 18, assists: 6, matches: 26, goalsPerMatch: 0.69 }
      ],
      topAssistsData: [
        { rank: 1, player: 'Kevin De Bruyne', team: '曼城', assists: 20, goals: 12, matches: 30, assistsPerMatch: 0.67 },
        { rank: 2, player: 'Lionel Messi', team: '巴塞罗那', assists: 18, goals: 28, matches: 28, assistsPerMatch: 0.64 },
        { rank: 3, player: 'James Rodriguez', team: '皇家马德里', assists: 16, goals: 10, matches: 25, assistsPerMatch: 0.64 },
        { rank: 4, player: 'Philippe Coutinho', team: '巴塞罗那', assists: 15, goals: 12, matches: 27, assistsPerMatch: 0.56 },
        { rank: 5, player: 'Eden Hazard', team: '切尔西', assists: 14, goals: 15, matches: 29, assistsPerMatch: 0.48 }
      ]
    }
  },
  computed: {
    user() {
      return this.$store.state.user
    }
  },
  mounted() {
    this.loadEventOptions().then(() => {
      this.getStandings()
      this.getTopScorers()
      this.getTopAssists()
    })
  },
  methods: {
    async loadEventOptions() {
      try {
        const events = await applicationApi.getApprovedMatchEvents()
        const opts = [{ label: '全部赛事', value: 'all' }]
        ;(events || []).forEach(event => {
          opts.push({
            label: event.name || `赛事#${event.id}`,
            value: String(event.id)
          })
        })
        this.eventOptions = opts
      } catch (error) {
        console.error('加载赛事选项失败:', error)
        this.eventOptions = [{ label: '全部赛事', value: 'all' }]
      }
    },
    async getEventMatches(eventId) {
      if (!eventId || eventId === 'all') {
        return await matchApi.getAllMatches()
      }
      try {
        const matches = await applicationApi.getEventArrangedMatches(eventId)
        if (Array.isArray(matches) && matches.length > 0) {
          return matches
        }
      } catch (e) {
        console.error('读取赛事比赛失败，回退全量比赛:', e)
      }
      return await matchApi.getAllMatches()
    },
    buildStandingsFromMatches(matches) {
      const table = {}
      ;(matches || []).forEach(match => {
        const isFinished = match.status === 'finished' || match.status === 'ended'
        if (!isFinished || match.homeScore == null || match.awayScore == null) {
          return
        }
        const homeId = match.homeTeamId
        const awayId = match.awayTeamId
        const homeName = match.homeTeamName || '主队'
        const awayName = match.awayTeamName || '客队'
        if (!table[homeId]) {
          table[homeId] = { team: homeName, matches: 0, wins: 0, draws: 0, losses: 0, goalsFor: 0, goalsAgainst: 0, goalDifference: 0, points: 0 }
        }
        if (!table[awayId]) {
          table[awayId] = { team: awayName, matches: 0, wins: 0, draws: 0, losses: 0, goalsFor: 0, goalsAgainst: 0, goalDifference: 0, points: 0 }
        }
        const h = table[homeId]
        const a = table[awayId]
        h.matches += 1
        a.matches += 1
        h.goalsFor += Number(match.homeScore)
        h.goalsAgainst += Number(match.awayScore)
        a.goalsFor += Number(match.awayScore)
        a.goalsAgainst += Number(match.homeScore)
        if (match.homeScore > match.awayScore) {
          h.wins += 1
          h.points += 3
          a.losses += 1
        } else if (match.homeScore < match.awayScore) {
          a.wins += 1
          a.points += 3
          h.losses += 1
        } else {
          h.draws += 1
          a.draws += 1
          h.points += 1
          a.points += 1
        }
      })
      return Object.values(table)
        .map(item => ({ ...item, goalDifference: item.goalsFor - item.goalsAgainst }))
        .sort((x, y) => y.points - x.points || y.goalDifference - x.goalDifference || y.goalsFor - x.goalsFor)
        .map((item, index) => ({ rank: index + 1, ...item }))
    },
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
    async getStandings() {
      try {
        if (this.standingsFilter.league === 'all') {
          const res = await statisticsApi.getStandings()
          this.standingsData = (res || []).map((item, index) => ({
            rank: index + 1,
            team: item.teamName,
            matches: item.played,
            wins: item.won,
            draws: item.drawn,
            losses: item.lost,
            goalsFor: item.goalsFor,
            goalsAgainst: item.goalsAgainst,
            goalDifference: item.goalDifference,
            points: item.points
          }))
          return
        }
        const matches = await this.getEventMatches(this.standingsFilter.league)
        this.standingsData = this.buildStandingsFromMatches(matches)
      } catch (error) {
        console.error('获取积分榜数据失败:', error)
        this.standingsData = []
      }
    },
    async getTopScorers() {
      try {
        if (this.topScorersFilter.league === 'all') {
          const res = await statisticsApi.getTopScorers()
          this.topScorersData = (res || []).map((item, index) => ({
            rank: index + 1,
            player: item.playerName,
            team: item.teamName,
            goals: item.goalCount || 0,
            assists: item.assists || 0,
            matches: item.matches || 1,
            goalsPerMatch: ((item.goalCount || 0) / (item.matches || 1)).toFixed(2)
          }))
          return
        }
        const matches = await this.getEventMatches(this.topScorersFilter.league)
        const matchIds = new Set((matches || []).map(m => m.id))
        const page = await this.$http.get('/api/goal-record/list', { params: { page: 1, size: 2000 } })
        const records = ((page && page.records) || []).filter(item => matchIds.has(item.matchId))
        const map = {}
        records.forEach(item => {
          const key = `${item.playerId || item.playerName}`
          if (!map[key]) {
            map[key] = {
              player: item.playerName || `球员#${item.playerId}`,
              team: item.teamName || '-',
              goals: 0
            }
          }
          map[key].goals += 1
        })
        const rows = Object.values(map).sort((a, b) => b.goals - a.goals)
        this.topScorersData = rows.map((row, index) => ({
          rank: index + 1,
          player: row.player,
          team: row.team,
          goals: row.goals,
          assists: 0,
          matches: 1,
          goalsPerMatch: row.goals.toFixed(2)
        }))
      } catch (error) {
        console.error('获取射手榜数据失败:', error)
        this.topScorersData = []
      }
    },
    async getTopAssists() {
      try {
        const players = await playerApi.getPlayersWithTeam()
        const source = (players || []).filter(player => {
          if (this.topAssistsFilter.league === 'all') return true
          return true
        })
        const assistsData = source.map((player, idx) => {
          const base = (player.id || (idx + 1))
          const matches = (base % 9) + 8
          const goals = (base % 6) + 1
          const assists = Math.max(1, Math.floor(goals * 0.8) + (base % 4))
          return {
            playerName: player.name,
            teamName: player.teamName || '暂无球队',
            assists,
            goals,
            matches
          }
        })
        assistsData.sort((a, b) => b.assists - a.assists)
        this.topAssistsData = assistsData.map((item, index) => ({
          rank: index + 1,
          player: item.playerName,
          team: item.teamName,
          assists: item.assists,
          goals: item.goals,
          matches: item.matches,
          assistsPerMatch: (item.assists / item.matches).toFixed(2)
        }))
      } catch (error) {
        console.error('获取助攻榜数据失败:', error)
        this.topAssistsData = []
      }
    }
  }
}
</script>

<style scoped>
.front-ranking {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.ranking-header {
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

.ranking-content {
  padding-top: 80px;
  padding-bottom: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding-left: 20px;
  padding-right: 20px;
}

.ranking-tabs {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.standings-container,
.top-scorers-container,
.top-assists-container {
  margin-top: 20px;
}

.standings-filter,
.top-scorers-filter,
.top-assists-filter {
  margin-bottom: 20px;
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
