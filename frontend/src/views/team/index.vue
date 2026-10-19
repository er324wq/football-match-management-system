<template>
  <div class="team-management">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>球队管理</span>
          <el-button type="primary" @click="handleAdd">
            <i class="el-icon-plus"></i> 新增球队
          </el-button>
        </div>
      </template>

      <div class="search-form">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="球队名称">
            <el-input v-model="searchForm.name" placeholder="请输入球队名称"></el-input>
          </el-form-item>
          <el-form-item label="城市">
            <el-input v-model="searchForm.city" placeholder="请输入城市"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="teamList" style="width: 100%" @expand-change="handleExpandChange">
        <el-table-column type="expand" width="56">
          <template slot-scope="scope">
            <div class="member-panel">
              <div v-if="isTeamPlayersLoading(scope.row.id)" class="member-loading">正在加载队员...</div>
              <div v-else-if="getTeamPlayers(scope.row.id).length === 0" class="member-empty">该球队暂无队员</div>
              <el-table v-else :data="getTeamPlayers(scope.row.id)" size="mini" border>
                <el-table-column prop="id" label="ID" width="80"></el-table-column>
                <el-table-column prop="name" label="姓名" min-width="140"></el-table-column>
                <el-table-column prop="position" label="位置" width="120"></el-table-column>
                <el-table-column prop="jerseyNumber" label="球衣号" width="100"></el-table-column>
                <el-table-column prop="age" label="年龄" width="100"></el-table-column>
                <el-table-column prop="createTime" label="加入时间" min-width="180"></el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="name" label="球队名称" min-width="180"></el-table-column>
        <el-table-column prop="coach" label="教练" min-width="120"></el-table-column>
        <el-table-column prop="city" label="城市" min-width="120"></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template slot-scope="scope">
            <el-switch v-model="scope.row.status" @change="handleStatusChange(scope.row)"></el-switch>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180"></el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
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

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item label="球队名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入球队名称"></el-input>
        </el-form-item>
        <el-form-item label="教练" prop="coach">
          <el-input v-model="form.coach" placeholder="请输入教练姓名"></el-input>
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="请输入城市"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入球队描述"></el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status"></el-switch>
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
import * as teamApi from '@/api/team'
import * as playerApi from '@/api/player'

export default {
  name: 'TeamManagement',
  data() {
    return {
      teamList: [],
      teamPlayersMap: {},
      teamPlayersLoading: {},
      searchForm: {
        name: '',
        city: ''
      },
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: '',
        name: '',
        coach: '',
        city: '',
        description: '',
        status: 1
      },
      rules: {
        name: [{ required: true, message: '请输入球队名称', trigger: 'blur' }],
        coach: [{ required: true, message: '请输入教练姓名', trigger: 'blur' }],
        city: [{ required: true, message: '请输入城市', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    this.loadTeamList()
  },
  methods: {
    async loadTeamList() {
      const res = await teamApi.getTeamList({
        page: this.currentPage,
        size: this.pageSize,
        name: this.searchForm.name,
        city: this.searchForm.city
      })
      this.teamList = (res.records || []).map(team => ({
        ...team,
        status: team.status === 1
      }))
      this.total = res.total || 0
    },
    async loadTeamPlayers(teamId) {
      if (!teamId || this.teamPlayersLoading[teamId]) {
        return
      }
      if (this.teamPlayersMap[teamId]) {
        return
      }
      this.$set(this.teamPlayersLoading, teamId, true)
      try {
        const players = await playerApi.getPlayersByTeam(teamId)
        this.$set(this.teamPlayersMap, teamId, players || [])
      } catch (error) {
        this.$set(this.teamPlayersMap, teamId, [])
        this.$message.error('加载队员失败')
      } finally {
        this.$set(this.teamPlayersLoading, teamId, false)
      }
    },
    handleExpandChange(row, expandedRows) {
      const isExpanded = expandedRows.some(item => item.id === row.id)
      if (isExpanded) {
        this.loadTeamPlayers(row.id)
      }
    },
    getTeamPlayers(teamId) {
      return this.teamPlayersMap[teamId] || []
    },
    isTeamPlayersLoading(teamId) {
      return !!this.teamPlayersLoading[teamId]
    },
    handleSearch() {
      this.currentPage = 1
      this.loadTeamList()
    },
    resetForm() {
      this.searchForm = {
        name: '',
        city: ''
      }
      this.currentPage = 1
      this.loadTeamList()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.loadTeamList()
    },
    handleCurrentChange(current) {
      this.currentPage = current
      this.loadTeamList()
    },
    handleAdd() {
      this.dialogTitle = '新增球队'
      this.form = {
        id: '',
        name: '',
        coach: '',
        city: '',
        description: '',
        status: 1
      }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑球队'
      this.form = Object.assign({}, row)
      this.dialogVisible = true
    },
    handleSubmit() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) {
          return
        }
        try {
          const submitData = Object.assign({}, this.form)
          submitData.status = this.form.status ? 1 : 0

          if (this.form.id) {
            await teamApi.updateTeam(submitData)
            this.$message.success('更新成功')
          } else {
            await teamApi.addTeam(submitData)
            this.$message.success('添加成功')
          }
          this.dialogVisible = false
          this.loadTeamList()
        } catch (error) {
          this.$message.error('操作失败：' + (error.message || '未知错误'))
        }
      })
    },
    handleDelete(id) {
      this.$confirm('确定要删除这个球队吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await teamApi.deleteTeam(id)
        this.$message.success('删除成功')
        this.loadTeamList()
      }).catch(() => {})
    },
    handleStatusChange(row) {
      const submitData = Object.assign({}, row)
      submitData.status = row.status ? 1 : 0

      teamApi.updateTeam(submitData).then(() => {
        this.$message.success('状态更新成功')
      }).catch(() => {
        row.status = !row.status
        this.$message.error('状态更新失败')
      })
    }
  }
}
</script>

<style scoped>
.team-management {
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

.member-panel {
  padding: 8px 16px;
  background: #fafafa;
}

.member-loading {
  color: #909399;
  line-height: 32px;
}

.member-empty {
  color: #909399;
  line-height: 32px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style>
