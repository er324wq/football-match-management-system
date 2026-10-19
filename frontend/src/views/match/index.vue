<template>
  <div class="match-management">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>比赛管理</span>
          <el-button type="primary" @click="handleAdd">
            <i class="el-icon-plus"></i> 新增比赛
          </el-button>
        </div>
      </template>

      <div class="search-form">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="比赛状态">
            <el-select v-model="searchForm.status" placeholder="请选择状态">
              <el-option label="全部" value=""></el-option>
              <el-option label="未开始" value="upcoming"></el-option>
              <el-option label="进行中" value="ongoing"></el-option>
              <el-option label="已结束" value="finished"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="matchList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column label="比赛标签" width="300">
          <template slot-scope="scope">
            <el-tag type="primary" size="small" effect="dark">{{ scope.row.tag || (scope.row.homeTeamName + ' vs ' + scope.row.awayTeamName + ' (' + scope.row.matchTime + ')') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="比赛信息" width="300">
          <template slot-scope="scope">
            <div style="white-space: nowrap;">{{ scope.row.homeTeamName }} vs {{ scope.row.awayTeamName }}</div>
            <div style="font-size: 12px; color: #909399;">{{ scope.row.matchTime }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="stadium" label="比赛场地"></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 'upcoming'" type="info">未开始</el-tag>
            <el-tag v-else-if="scope.row.status === 'ongoing'" type="warning">进行中</el-tag>
            <el-tag v-else type="success">已结束</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="比分" width="120">
          <template slot-scope="scope">
            {{ (scope.row.homeScore !== null && scope.row.homeScore !== undefined) ? scope.row.homeScore : 0 }} : {{ (scope.row.awayScore !== null && scope.row.awayScore !== undefined) ? scope.row.awayScore : 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180"></el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
        ></el-pagination>
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item label="主队" prop="homeTeamId">
          <el-select v-model="form.homeTeamId" placeholder="请选择主队" @change="generateMatchTag">
            <el-option v-for="team in teams" :key="team.id" :label="team.name" :value="team.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="客队" prop="awayTeamId">
          <el-select v-model="form.awayTeamId" placeholder="请选择客队" @change="generateMatchTag">
            <el-option v-for="team in teams" :key="team.id" :label="team.name" :value="team.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="比赛时间" prop="matchTime">
          <el-date-picker
            v-model="form.matchTime"
            type="datetime"
            placeholder="选择日期时间"
            style="width: 100%"
            @change="generateMatchTag"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="比赛场地" prop="stadium">
          <el-input v-model="form.stadium" placeholder="请输入比赛场地"></el-input>
        </el-form-item>
        <el-form-item label="比赛标签" prop="tag">
          <el-input v-model="form.tag" placeholder="自动生成，格式：主队 vs 客队 (时间)"></el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" @change="handleStatusChange">
            <el-option label="未开始" value="upcoming"></el-option>
            <el-option label="进行中" value="ongoing"></el-option>
            <el-option label="已结束" value="finished"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="主队比分" prop="homeScore" v-if="form.status !== 'upcoming'">
          <el-input-number v-model="form.homeScore" :min="0" :max="99" placeholder="主队比分"></el-input-number>
        </el-form-item>
        <el-form-item label="客队比分" prop="awayScore" v-if="form.status !== 'upcoming'">
          <el-input-number v-model="form.awayScore" :min="0" :max="99" placeholder="客队比分"></el-input-number>
        </el-form-item>
        <el-form-item label="裁判" prop="referee" v-if="form.status !== 'upcoming'">
          <el-input v-model="form.referee" placeholder="请输入裁判姓名"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import * as matchApi from '@/api/match'
import * as teamApi from '@/api/team'

export default {
  name: 'MatchManagement',
  data() {
    return {
      matchList: [],
      teams: [],
      searchForm: {
        status: ''
      },
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: '',
        homeTeamId: '',
        awayTeamId: '',
        matchTime: '',
        stadium: '',
        status: 'upcoming',
        homeScore: 0,
        awayScore: 0,
        referee: ''
      },
      rules: {
        homeTeamId: [
          { required: true, message: '请选择主队', trigger: 'change' }
        ],
        awayTeamId: [
          { required: true, message: '请选择客队', trigger: 'change' }
        ],
        matchTime: [
          { required: true, message: '请选择比赛时间', trigger: 'change' }
        ],
        stadium: [
          { required: true, message: '请输入比赛场地', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '请选择状态', trigger: 'change' }
        ]
      }
    }
  },
  mounted() {
    this.loadTeams()
    this.loadMatchList()
  },
  methods: {
    loadTeams() {
      teamApi.getAllTeams().then(res => {
        this.teams = res
      })
    },
    loadMatchList() {
      matchApi.getMatchList({
        page: this.currentPage,
        size: this.pageSize,
        status: this.searchForm.status
      }).then(res => {
        this.matchList = res.records
        this.total = res.total
      })
    },
    handleSearch() {
      this.currentPage = 1
      this.loadMatchList()
    },
    resetForm() {
      this.searchForm = {
        status: ''
      }
      this.currentPage = 1
      this.loadMatchList()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.loadMatchList()
    },
    handleCurrentChange(current) {
      this.currentPage = current
      this.loadMatchList()
    },
    handleAdd() {
      this.dialogTitle = '新增比赛'
      this.form = {
        id: '',
        homeTeamId: '',
        awayTeamId: '',
        matchTime: '',
        stadium: '',
        status: 'upcoming',
        tag: ''
      }
      this.dialogVisible = true
    },
    handleStatusChange(value) {
      if (value === 'upcoming') {
        this.form.homeScore = 0
        this.form.awayScore = 0
        this.form.referee = ''
      }
    },
    // 自动生成比赛标签
    generateMatchTag() {
      if (this.form.homeTeamId && this.form.awayTeamId && this.form.matchTime) {
        const homeTeam = this.teams.find(team => team.id === this.form.homeTeamId)
        const awayTeam = this.teams.find(team => team.id === this.form.awayTeamId)
        if (homeTeam && awayTeam) {
          const matchTime = new Date(this.form.matchTime).toLocaleString()
          this.form.tag = `${homeTeam.name} vs ${awayTeam.name} (${matchTime})`
        }
      }
    },
    handleEdit(row) {
      this.dialogTitle = '编辑比赛'
      this.form = Object.assign({}, row)
      this.dialogVisible = true
    },
    handleSubmit() {
      this.$refs.formRef.validate(async (valid) => {
        if (valid) {
          try {
            if (this.form.id) {
              await matchApi.updateMatch(this.form)
              this.$message.success('更新成功')
            } else {
              await matchApi.addMatch(this.form)
              this.$message.success('添加成功')
            }
            this.dialogVisible = false
            this.loadMatchList()
          } catch (error) {
            console.error('操作失败:', error)
            this.$message.error('操作失败：' + (error.message || '未知错误'))
          }
        }
      })
    },
    handleDelete(id) {
      this.$confirm('确定要删除这个比赛吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await matchApi.deleteMatch(id)
          this.$message.success('删除成功')
          this.loadMatchList()
        } catch (error) {
          console.error('删除失败:', error)
        }
      })
    }
  }
}
</script>

<style scoped>
.match-management {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style>
