<template>
  <div class="player-management">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>球员管理</span>
          <el-button type="primary" @click="handleAdd">
            <i class="el-icon-plus"></i> 新增球员
          </el-button>
        </div>
      </template>

      <div class="search-form">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="球员姓名">
            <el-input v-model="searchForm.name" placeholder="请输入球员姓名"></el-input>
          </el-form-item>
          <el-form-item label="所属球队">
            <el-select v-model="searchForm.teamId" placeholder="请选择球队">
              <el-option label="全部" value=""></el-option>
              <el-option v-for="team in teams" :key="team.id" :label="team.name" :value="team.id"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="playerList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="name" label="姓名"></el-table-column>
        <el-table-column prop="age" label="年龄" width="80"></el-table-column>
        <el-table-column prop="position" label="位置" width="100"></el-table-column>
        <el-table-column prop="jerseyNumber" label="球衣号码" width="100"></el-table-column>
        <el-table-column prop="teamName" label="所属球队"></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template slot-scope="scope">
            <el-switch v-model="scope.row.status" @change="handleStatusChange(scope.row)"></el-switch>
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入球员姓名"></el-input>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input v-model="form.age" type="number" placeholder="请输入年龄"></el-input>
        </el-form-item>
        <el-form-item label="位置" prop="position">
          <el-select v-model="form.position" placeholder="请选择位置">
            <el-option label="前锋" value="前锋"></el-option>
            <el-option label="中场" value="中场"></el-option>
            <el-option label="后卫" value="后卫"></el-option>
            <el-option label="守门员" value="守门员"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="球衣号码" prop="jerseyNumber">
          <el-input v-model="form.jerseyNumber" type="number" placeholder="请输入球衣号码"></el-input>
        </el-form-item>
        <el-form-item label="所属球队" prop="teamId">
          <el-select v-model="form.teamId" placeholder="请选择球队">
            <el-option v-for="team in teams" :key="team.id" :label="team.name" :value="team.id"></el-option>
          </el-select>
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
import * as playerApi from '@/api/player'
import * as teamApi from '@/api/team'

export default {
  name: 'PlayerManagement',
  data() {
    return {
      playerList: [],
      teams: [],
      searchForm: {
        name: '',
        teamId: ''
      },
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: '',
        name: '',
        age: '',
        position: '',
        jerseyNumber: '',
        teamId: '',
        status: 1
      },
      rules: {
        name: [
          { required: true, message: '请输入球员姓名', trigger: 'blur' }
        ],
        age: [
          { required: true, message: '请输入年龄', trigger: 'blur' }
        ],
        position: [
          { required: true, message: '请选择位置', trigger: 'change' }
        ],
        jerseyNumber: [
          { required: true, message: '请输入球衣号码', trigger: 'blur' }
        ],
        teamId: [
          { required: true, message: '请选择所属球队', trigger: 'change' }
        ]
      }
    }
  },
  mounted() {
    this.loadTeams()
    this.loadPlayerList()
  },
  methods: {
    loadTeams() {
      teamApi.getAllTeams().then(res => {
        this.teams = res
      })
    },
    loadPlayerList() {
      playerApi.getPlayersWithTeam().then(res => {
        // 将status转换为布尔值，确保el-switch正确显示
        let filteredList = res.map(player => ({
          ...player,
          status: player.status === 1
        }))
        // 过滤数据
        if (this.searchForm.name) {
          filteredList = filteredList.filter(player => player.name.includes(this.searchForm.name))
        }
        if (this.searchForm.teamId) {
          filteredList = filteredList.filter(player => player.teamId === this.searchForm.teamId)
        }
        // 分页处理
        this.total = filteredList.length
        const start = (this.currentPage - 1) * this.pageSize
        const end = start + this.pageSize
        this.playerList = filteredList.slice(start, end)
      })
    },
    handleSearch() {
      this.currentPage = 1
      this.loadPlayerList()
    },
    resetForm() {
      this.searchForm = {
        name: '',
        teamId: ''
      }
      this.currentPage = 1
      this.loadPlayerList()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.loadPlayerList()
    },
    handleCurrentChange(current) {
      this.currentPage = current
      this.loadPlayerList()
    },
    handleAdd() {
      this.dialogTitle = '新增球员'
      this.form = {
        id: '',
        name: '',
        age: '',
        position: '',
        jerseyNumber: '',
        teamId: '',
        status: 1
      }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑球员'
      this.form = Object.assign({}, row)
      this.dialogVisible = true
    },
    handleSubmit() {
      this.$refs.formRef.validate(async (valid) => {
        if (valid) {
          try {
            // 将status从布尔值转换为整数
            const submitData = Object.assign({}, this.form)
            submitData.status = this.form.status ? 1 : 0

            if (this.form.id) {
              await playerApi.updatePlayer(submitData)
              this.$message.success('更新成功')
            } else {
              await playerApi.addPlayer(submitData)
              this.$message.success('添加成功')
            }
            this.dialogVisible = false
            this.loadPlayerList()
          } catch (error) {
            console.error('操作失败:', error)
            this.$message.error('操作失败：' + (error.message || '未知错误'))
          }
        }
      })
    },
    handleDelete(id) {
      this.$confirm('确定要删除这个球员吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await playerApi.deletePlayer(id)
          this.$message.success('删除成功')
          this.loadPlayerList()
        } catch (error) {
          console.error('删除失败:', error)
        }
      })
    },
    handleStatusChange(row) {
      // 将status从布尔值转换为整数
      const submitData = Object.assign({}, row)
      submitData.status = row.status ? 1 : 0

      playerApi.updatePlayer(submitData).then(() => {
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
.player-management {
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
