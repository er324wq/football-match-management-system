<template>
  <div class="match-record-management">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>比赛记录管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="比赛结果" name="result">
          <div class="toolbar">
            <el-button type="primary" @click="openResultDialog()">
              <i class="el-icon-plus"></i> 录入比赛结果
            </el-button>
          </div>
          <el-table :data="matchResultList" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70"></el-table-column>
            <el-table-column label="比赛" min-width="240">
              <template slot-scope="scope">
                {{ scope.row.homeTeamName }} vs {{ scope.row.awayTeamName }}
              </template>
            </el-table-column>
            <el-table-column label="比分" width="120">
              <template slot-scope="scope">
                <b>{{ scope.row.homeScore }} : {{ scope.row.awayScore }}</b>
              </template>
            </el-table-column>
            <el-table-column prop="referee" label="裁判" width="160"></el-table-column>
            <el-table-column prop="updateTime" label="更新时间" width="180"></el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template slot-scope="scope">
                <el-button size="mini" type="primary" @click="openResultDialog(scope.row)">编辑</el-button>
                <el-button size="mini" type="danger" @click="removeResult(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="进球记录" name="goal">
          <div class="toolbar">
            <el-form :inline="true" :model="goalSearchForm">
              <el-form-item label="比赛">
                <el-select v-model="goalSearchForm.matchId" placeholder="全部比赛" clearable>
                  <el-option v-for="item in matches" :key="item.id" :label="formatMatch(item)" :value="item.id"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="球员">
                <el-select v-model="goalSearchForm.playerId" placeholder="全部球员" clearable>
                  <el-option v-for="item in players" :key="item.id" :label="item.name" :value="item.id"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchGoals">查询</el-button>
                <el-button @click="resetGoalSearch">重置</el-button>
              </el-form-item>
              <el-form-item>
                <el-button type="success" @click="openGoalDialog()">
                  <i class="el-icon-plus"></i> 录入进球
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table :data="goalRecordList" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70"></el-table-column>
            <el-table-column prop="matchInfo" label="比赛" min-width="220"></el-table-column>
            <el-table-column prop="playerName" label="球员" width="140"></el-table-column>
            <el-table-column prop="teamName" label="球队" width="140"></el-table-column>
            <el-table-column label="进球时间" width="120">
              <template slot-scope="scope">{{ scope.row.goalTime }} 分钟</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template slot-scope="scope">
                <el-button size="mini" type="primary" @click="openGoalDialog(scope.row)">编辑</el-button>
                <el-button size="mini" type="danger" @click="removeGoal(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              @size-change="handleGoalSizeChange"
              @current-change="handleGoalCurrentChange"
              :current-page="goalPage.page"
              :page-sizes="[10, 20, 50, 100]"
              :page-size="goalPage.size"
              layout="total, sizes, prev, pager, next, jumper"
              :total="goalPage.total"
            ></el-pagination>
          </div>
        </el-tab-pane>

        <el-tab-pane label="红黄牌信息" name="card">
          <div class="toolbar">
            <el-form :inline="true" :model="cardSearchForm">
              <el-form-item label="比赛">
                <el-select v-model="cardSearchForm.matchId" placeholder="全部比赛" clearable>
                  <el-option v-for="item in matches" :key="item.id" :label="formatMatch(item)" :value="item.id"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="球员">
                <el-select v-model="cardSearchForm.playerId" placeholder="全部球员" clearable>
                  <el-option v-for="item in players" :key="item.id" :label="item.name" :value="item.id"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="牌类型">
                <el-select v-model="cardSearchForm.cardType" placeholder="全部" clearable>
                  <el-option label="黄牌" value="yellow"></el-option>
                  <el-option label="红牌" value="red"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchCards">查询</el-button>
                <el-button @click="resetCardSearch">重置</el-button>
              </el-form-item>
              <el-form-item>
                <el-button type="warning" @click="openCardDialog()">
                  <i class="el-icon-plus"></i> 录入红黄牌
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table :data="cardRecordList" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70"></el-table-column>
            <el-table-column prop="matchInfo" label="比赛" min-width="220"></el-table-column>
            <el-table-column prop="playerName" label="球员" width="140"></el-table-column>
            <el-table-column prop="teamName" label="球队" width="140"></el-table-column>
            <el-table-column label="牌类型" width="100">
              <template slot-scope="scope">
                <el-tag :type="scope.row.cardType === 'red' ? 'danger' : 'warning'">
                  {{ scope.row.cardType === 'red' ? '红牌' : '黄牌' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="100">
              <template slot-scope="scope">{{ scope.row.cardTime }} 分钟</template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip></el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template slot-scope="scope">
                <el-button size="mini" type="primary" @click="openCardDialog(scope.row)">编辑</el-button>
                <el-button size="mini" type="danger" @click="removeCard(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              @size-change="handleCardSizeChange"
              @current-change="handleCardCurrentChange"
              :current-page="cardPage.page"
              :page-sizes="[10, 20, 50, 100]"
              :page-size="cardPage.size"
              layout="total, sizes, prev, pager, next, jumper"
              :total="cardPage.total"
            ></el-pagination>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog :title="resultDialogTitle" :visible.sync="resultDialogVisible" width="520px">
      <el-form :model="resultForm" :rules="resultRules" ref="resultFormRef" label-width="100px">
        <el-form-item label="比赛" prop="matchId">
          <el-select v-model="resultForm.matchId" placeholder="请选择比赛">
            <el-option v-for="item in matches" :key="item.id" :label="formatMatch(item)" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="主队比分" prop="homeScore">
          <el-input-number v-model="resultForm.homeScore" :min="0"></el-input-number>
        </el-form-item>
        <el-form-item label="客队比分" prop="awayScore">
          <el-input-number v-model="resultForm.awayScore" :min="0"></el-input-number>
        </el-form-item>
        <el-form-item label="裁判" prop="referee">
          <el-input v-model="resultForm.referee" placeholder="请输入裁判姓名"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="resultDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResult">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="goalDialogTitle" :visible.sync="goalDialogVisible" width="520px">
      <el-form :model="goalForm" :rules="goalRules" ref="goalFormRef" label-width="100px">
        <el-form-item label="比赛" prop="matchId">
          <el-select v-model="goalForm.matchId" placeholder="请选择比赛">
            <el-option v-for="item in matches" :key="item.id" :label="formatMatch(item)" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="球员" prop="playerId">
          <el-select v-model="goalForm.playerId" placeholder="请选择球员">
            <el-option v-for="item in players" :key="item.id" :label="item.name" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="进球时间" prop="goalTime">
          <el-input-number v-model="goalForm.goalTime" :min="1" :max="130"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="goalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGoal">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="cardDialogTitle" :visible.sync="cardDialogVisible" width="560px">
      <el-form :model="cardForm" :rules="cardRules" ref="cardFormRef" label-width="100px">
        <el-form-item label="比赛" prop="matchId">
          <el-select v-model="cardForm.matchId" placeholder="请选择比赛">
            <el-option v-for="item in matches" :key="item.id" :label="formatMatch(item)" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="球员" prop="playerId">
          <el-select v-model="cardForm.playerId" placeholder="请选择球员">
            <el-option v-for="item in players" :key="item.id" :label="item.name" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="牌类型" prop="cardType">
          <el-select v-model="cardForm.cardType" placeholder="请选择牌类型">
            <el-option label="黄牌" value="yellow"></el-option>
            <el-option label="红牌" value="red"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="判罚时间" prop="cardTime">
          <el-input-number v-model="cardForm.cardTime" :min="1" :max="130"></el-input-number>
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="cardForm.reason" type="textarea" :rows="3" placeholder="请输入判罚原因"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="cardDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCard">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import * as matchApi from '@/api/match'
import * as playerApi from '@/api/player'
import * as matchResultApi from '@/api/matchResult'
import * as goalRecordApi from '@/api/goalRecord'
import * as cardRecordApi from '@/api/cardRecord'

export default {
  name: 'MatchRecordManagement',
  data() {
    return {
      activeTab: 'result',
      matches: [],
      players: [],
      matchResultList: [],
      goalRecordList: [],
      cardRecordList: [],

      goalSearchForm: {
        matchId: null,
        playerId: null
      },
      cardSearchForm: {
        matchId: null,
        playerId: null,
        cardType: ''
      },
      goalPage: {
        page: 1,
        size: 10,
        total: 0
      },
      cardPage: {
        page: 1,
        size: 10,
        total: 0
      },

      resultDialogVisible: false,
      resultDialogTitle: '录入比赛结果',
      resultForm: {
        id: null,
        matchId: null,
        homeScore: 0,
        awayScore: 0,
        referee: ''
      },
      resultRules: {
        matchId: [{ required: true, message: '请选择比赛', trigger: 'change' }],
        homeScore: [{ required: true, message: '请输入主队比分', trigger: 'blur' }],
        awayScore: [{ required: true, message: '请输入客队比分', trigger: 'blur' }],
        referee: [{ required: true, message: '请输入裁判', trigger: 'blur' }]
      },

      goalDialogVisible: false,
      goalDialogTitle: '录入进球记录',
      goalForm: {
        id: null,
        matchId: null,
        playerId: null,
        goalTime: 1
      },
      goalRules: {
        matchId: [{ required: true, message: '请选择比赛', trigger: 'change' }],
        playerId: [{ required: true, message: '请选择球员', trigger: 'change' }],
        goalTime: [{ required: true, message: '请输入进球时间', trigger: 'blur' }]
      },

      cardDialogVisible: false,
      cardDialogTitle: '录入红黄牌信息',
      cardForm: {
        id: null,
        matchId: null,
        playerId: null,
        cardType: 'yellow',
        cardTime: 1,
        reason: ''
      },
      cardRules: {
        matchId: [{ required: true, message: '请选择比赛', trigger: 'change' }],
        playerId: [{ required: true, message: '请选择球员', trigger: 'change' }],
        cardType: [{ required: true, message: '请选择牌类型', trigger: 'change' }],
        cardTime: [{ required: true, message: '请输入判罚时间', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    this.initPage()
  },
  methods: {
    async initPage() {
      await Promise.all([this.loadMatches(), this.loadPlayers()])
      await Promise.all([this.loadMatchResults(), this.loadGoalRecords(), this.loadCardRecords()])
    },
    formatMatch(match) {
      if (!match) return '-'
      if (match.tag) return match.tag
      return `${match.homeTeamName || '-'} vs ${match.awayTeamName || '-'}`
    },
    async loadMatches() {
      this.matches = await matchApi.getAllMatches()
    },
    async loadPlayers() {
      this.players = await playerApi.getAllPlayers()
    },
    async loadMatchResults() {
      const res = await matchResultApi.getMatchResultList({ page: 1, size: 500 })
      this.matchResultList = res.records || []
    },
    async loadGoalRecords() {
      const params = {
        page: this.goalPage.page,
        size: this.goalPage.size
      }
      if (this.goalSearchForm.matchId) params.matchId = this.goalSearchForm.matchId
      if (this.goalSearchForm.playerId) params.playerId = this.goalSearchForm.playerId
      const res = await goalRecordApi.getGoalRecords(params)
      this.goalRecordList = res.records || []
      this.goalPage.total = res.total || 0
    },
    async loadCardRecords() {
      const params = {
        page: this.cardPage.page,
        size: this.cardPage.size
      }
      if (this.cardSearchForm.matchId) params.matchId = this.cardSearchForm.matchId
      if (this.cardSearchForm.playerId) params.playerId = this.cardSearchForm.playerId
      if (this.cardSearchForm.cardType) params.cardType = this.cardSearchForm.cardType
      const res = await cardRecordApi.getCardRecords(params)
      this.cardRecordList = res.records || []
      this.cardPage.total = res.total || 0
    },

    openResultDialog(row) {
      if (row) {
        this.resultDialogTitle = '编辑比赛结果'
        this.resultForm = {
          id: row.id,
          matchId: row.matchId,
          homeScore: row.homeScore,
          awayScore: row.awayScore,
          referee: row.referee
        }
      } else {
        this.resultDialogTitle = '录入比赛结果'
        this.resultForm = {
          id: null,
          matchId: null,
          homeScore: 0,
          awayScore: 0,
          referee: ''
        }
      }
      this.resultDialogVisible = true
    },
    submitResult() {
      this.$refs.resultFormRef.validate(async valid => {
        if (!valid) return
        try {
          const exists = this.matchResultList.find(item => item.matchId === this.resultForm.matchId)
          const payload = { ...this.resultForm }
          if (!payload.id && exists) {
            payload.id = exists.id
          }
          if (payload.id) {
            await matchResultApi.updateMatchResult(payload)
          } else {
            await matchResultApi.addMatchResult(payload)
          }
          this.$message.success('比赛结果保存成功')
          this.resultDialogVisible = false
          this.loadMatchResults()
        } catch (e) {
          this.$message.error('比赛结果保存失败')
        }
      })
    },
    async removeResult(id) {
      try {
        await this.$confirm('确认删除该比赛结果？', '提示', { type: 'warning' })
        await matchResultApi.deleteMatchResult(id)
        this.$message.success('删除成功')
        this.loadMatchResults()
      } catch (e) {}
    },

    openGoalDialog(row) {
      if (row) {
        this.goalDialogTitle = '编辑进球记录'
        this.goalForm = {
          id: row.id,
          matchId: row.matchId,
          playerId: row.playerId,
          goalTime: row.goalTime
        }
      } else {
        this.goalDialogTitle = '录入进球记录'
        this.goalForm = {
          id: null,
          matchId: null,
          playerId: null,
          goalTime: 1
        }
      }
      this.goalDialogVisible = true
    },
    submitGoal() {
      this.$refs.goalFormRef.validate(async valid => {
        if (!valid) return
        try {
          if (this.goalForm.id) {
            await goalRecordApi.updateGoalRecord(this.goalForm.id, this.goalForm)
          } else {
            await goalRecordApi.createGoalRecord(this.goalForm)
          }
          this.$message.success('进球记录保存成功')
          this.goalDialogVisible = false
          this.loadGoalRecords()
        } catch (e) {
          this.$message.error('进球记录保存失败')
        }
      })
    },
    async removeGoal(id) {
      try {
        await this.$confirm('确认删除该进球记录？', '提示', { type: 'warning' })
        await goalRecordApi.deleteGoalRecord(id)
        this.$message.success('删除成功')
        this.loadGoalRecords()
      } catch (e) {}
    },
    searchGoals() {
      this.goalPage.page = 1
      this.loadGoalRecords()
    },
    resetGoalSearch() {
      this.goalSearchForm = { matchId: null, playerId: null }
      this.goalPage.page = 1
      this.loadGoalRecords()
    },
    handleGoalSizeChange(size) {
      this.goalPage.size = size
      this.goalPage.page = 1
      this.loadGoalRecords()
    },
    handleGoalCurrentChange(page) {
      this.goalPage.page = page
      this.loadGoalRecords()
    },

    openCardDialog(row) {
      if (row) {
        this.cardDialogTitle = '编辑红黄牌信息'
        this.cardForm = {
          id: row.id,
          matchId: row.matchId,
          playerId: row.playerId,
          cardType: row.cardType,
          cardTime: row.cardTime,
          reason: row.reason || ''
        }
      } else {
        this.cardDialogTitle = '录入红黄牌信息'
        this.cardForm = {
          id: null,
          matchId: null,
          playerId: null,
          cardType: 'yellow',
          cardTime: 1,
          reason: ''
        }
      }
      this.cardDialogVisible = true
    },
    submitCard() {
      this.$refs.cardFormRef.validate(async valid => {
        if (!valid) return
        try {
          if (this.cardForm.id) {
            await cardRecordApi.updateCardRecord(this.cardForm)
          } else {
            await cardRecordApi.createCardRecord(this.cardForm)
          }
          this.$message.success('红黄牌信息保存成功')
          this.cardDialogVisible = false
          this.loadCardRecords()
        } catch (e) {
          this.$message.error('红黄牌信息保存失败')
        }
      })
    },
    async removeCard(id) {
      try {
        await this.$confirm('确认删除该红黄牌记录？', '提示', { type: 'warning' })
        await cardRecordApi.deleteCardRecord(id)
        this.$message.success('删除成功')
        this.loadCardRecords()
      } catch (e) {}
    },
    searchCards() {
      this.cardPage.page = 1
      this.loadCardRecords()
    },
    resetCardSearch() {
      this.cardSearchForm = { matchId: null, playerId: null, cardType: '' }
      this.cardPage.page = 1
      this.loadCardRecords()
    },
    handleCardSizeChange(size) {
      this.cardPage.size = size
      this.cardPage.page = 1
      this.loadCardRecords()
    },
    handleCardCurrentChange(page) {
      this.cardPage.page = page
      this.loadCardRecords()
    }
  }
}
</script>

<style scoped>
.match-record-management {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
