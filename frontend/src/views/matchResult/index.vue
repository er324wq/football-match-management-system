<template>
  <div class="match-result-management">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>比赛结果管理</span>
          <el-button type="primary" @click="handleAdd">
            <i class="el-icon-plus"></i> 新增比赛结果
          </el-button>
        </div>
      </template>

      <div class="search-form">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="比赛">
            <el-select v-model="searchForm.matchId" placeholder="请选择比赛">
              <el-option label="全部" value=""></el-option>
              <el-option v-for="match in matches" :key="match.id" :label="match.homeTeamName + ' vs ' + match.awayTeamName" :value="match.id"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="matchResultList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column label="比赛" width="300">
          <template slot-scope="scope">
            {{ scope.row.homeTeamName }} vs {{ scope.row.awayTeamName }}
          </template>
        </el-table-column>
        <el-table-column label="比分" width="120">
          <template slot-scope="scope">
            <span style="font-weight: bold;">{{ scope.row.homeScore }} : {{ scope.row.awayScore }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="referee" label="裁判"></el-table-column>
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item label="比赛" prop="matchId">
          <el-select v-model="form.matchId" placeholder="请选择比赛">
            <el-option v-for="match in matches" :key="match.id" :label="match.homeTeamName + ' vs ' + match.awayTeamName" :value="match.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="主队比分" prop="homeScore">
          <el-input v-model="form.homeScore" type="number" placeholder="请输入主队比分"></el-input>
        </el-form-item>
        <el-form-item label="客队比分" prop="awayScore">
          <el-input v-model="form.awayScore" type="number" placeholder="请输入客队比分"></el-input>
        </el-form-item>
        <el-form-item label="裁判" prop="referee">
          <el-select v-model="form.referee" placeholder="请选择裁判">
            <el-option v-for="referee in referees" :key="referee.id" :label="referee.nickname" :value="referee.nickname"></el-option>
          </el-select>
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
import * as matchResultApi from '@/api/matchResult'
import * as matchApi from '@/api/match'
import * as userApi from '@/api/user'

export default {
  name: 'MatchResultManagement',
  data() {
    return {
      matchResultList: [],
      matches: [],
      referees: [],
      searchForm: {
        matchId: ''
      },
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: '',
        matchId: '',
        homeScore: '',
        awayScore: '',
        referee: ''
      },
      rules: {
        matchId: [
          { required: true, message: '请选择比赛', trigger: 'change' }
        ],
        homeScore: [
          { required: true, message: '请输入主队比分', trigger: 'blur' }
        ],
        awayScore: [
          { required: true, message: '请输入客队比分', trigger: 'blur' }
        ],
        referee: [
          { required: true, message: '请选择裁判', trigger: 'change' }
        ]
      }
    }
  },
  mounted() {
    this.loadMatches()
    this.loadReferees()
    this.loadMatchResultList()
  },
  methods: {
    loadMatches() {
      matchApi.getAllMatches().then(res => {
        this.matches = res
      })
    },
    loadReferees() {
      userApi.getUserList({ role: 'referee' }).then(res => {
        this.referees = res.records
      })
    },
    loadMatchResultList() {
      matchResultApi.getMatchResultList({
        page: this.currentPage,
        size: this.pageSize,
        matchId: this.searchForm.matchId
      }).then(res => {
        this.matchResultList = res.records
        this.total = res.total
      })
    },
    handleSearch() {
      this.currentPage = 1
      this.loadMatchResultList()
    },
    resetForm() {
      this.searchForm = {
        matchId: ''
      }
      this.currentPage = 1
      this.loadMatchResultList()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.loadMatchResultList()
    },
    handleCurrentChange(current) {
      this.currentPage = current
      this.loadMatchResultList()
    },
    handleAdd() {
      this.dialogTitle = '新增比赛结果'
      this.form = {
        id: '',
        matchId: '',
        homeScore: '',
        awayScore: '',
        referee: ''
      }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑比赛结果'
      this.form = Object.assign({}, row)
      this.dialogVisible = true
    },
    handleSubmit() {
      this.$refs.formRef.validate(async (valid) => {
        if (valid) {
          try {
            if (this.form.id) {
              await matchResultApi.updateMatchResult(this.form)
              this.$message.success('更新成功')
            } else {
              await matchResultApi.addMatchResult(this.form)
              this.$message.success('添加成功')
            }
            this.dialogVisible = false
            this.loadMatchResultList()
          } catch (error) {
            console.error('操作失败:', error)
            this.$message.error('操作失败：' + (error.message || '未知错误'))
          }
        }
      })
    },
    handleDelete(id) {
      this.$confirm('确定要删除这个比赛结果吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await matchResultApi.deleteMatchResult(id)
          this.$message.success('删除成功')
          this.loadMatchResultList()
        } catch (error) {
          console.error('删除失败:', error)
        }
      })
    }
  }
}
</script>

<style scoped>
.match-result-management {
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
