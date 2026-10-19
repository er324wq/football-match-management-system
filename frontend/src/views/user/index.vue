<template>
  <div class="user-management">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd">
            <i class="el-icon-plus"></i> 新增用户
          </el-button>
        </div>
      </template>

      <div class="search-form">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="用户名">
            <el-input v-model="searchForm.username" placeholder="请输入用户名"></el-input>
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="searchForm.role" placeholder="请选择角色">
              <el-option label="管理员" value="admin"></el-option>
              <el-option label="裁判" value="referee"></el-option>
              <el-option label="运动员" value="player"></el-option>
              <el-option label="普通用户" value="user"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="userList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="username" label="用户名"></el-table-column>
        <el-table-column prop="nickname" label="昵称"></el-table-column>
        <el-table-column prop="phone" label="手机号" width="140"></el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180"></el-table-column>
        <el-table-column prop="signature" label="个性签名" min-width="160" show-overflow-tooltip></el-table-column>
        <el-table-column prop="avatar" label="头像URL" min-width="180" show-overflow-tooltip></el-table-column>
        <el-table-column prop="role" label="角色" width="120">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.role === 'admin'" type="danger">管理员</el-tag>
            <el-tag v-else-if="scope.row.role === 'referee'" type="warning">裁判</el-tag>
            <el-tag v-else-if="scope.row.role === 'player'" type="success">运动员</el-tag>
            <el-tag v-else type="info">普通用户</el-tag>
          </template>
        </el-table-column>
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
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" v-if="!form.id"></el-input>
          <el-input v-model="form.password" type="password" placeholder="不修改请留空" v-else></el-input>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        <el-form-item label="头像URL" prop="avatar">
          <el-input v-model="form.avatar" placeholder="请输入头像URL"></el-input>
        </el-form-item>
        <el-form-item label="个性签名" prop="signature">
          <el-input v-model="form.signature" type="textarea" :rows="2" placeholder="请输入个性签名"></el-input>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="管理员" value="admin"></el-option>
            <el-option label="裁判" value="referee"></el-option>
            <el-option label="运动员" value="player"></el-option>
            <el-option label="普通用户" value="user"></el-option>
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
import * as userApi from '@/api/user'

export default {
  name: 'UserManagement',
  data() {
    return {
      userList: [],
      searchForm: {
        username: '',
        role: ''
      },
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: '',
        username: '',
        password: '',
        nickname: '',
        phone: '',
        email: '',
        avatar: '',
        signature: '',
        role: 'user',
        status: 1
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur', validator: (rule, value, callback) => {
            if (!this.form.id && !value) {
              callback(new Error('请输入密码'))
            } else {
              callback()
            }
          }}
        ],
        nickname: [
          { required: true, message: '请输入昵称', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1\d{10}$/, message: '请输入有效的11位手机号', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入有效邮箱', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ]
      },
      autoRefreshTimer: null
    }
  },
  mounted() {
    this.loadUserList()
    this.autoRefreshTimer = setInterval(() => {
      this.loadUserList()
    }, 8000)
  },
  beforeDestroy() {
    if (this.autoRefreshTimer) {
      clearInterval(this.autoRefreshTimer)
      this.autoRefreshTimer = null
    }
  },
  methods: {
    loadUserList() {
      userApi.getUserList({
        page: this.currentPage,
        size: this.pageSize,
        username: this.searchForm.username,
        role: this.searchForm.role
      }).then(res => {
        // 将status转换为布尔值，确保el-switch正确显示
        this.userList = res.records.map(user => ({
          ...user,
          status: user.status === 1
        }))
        this.total = res.total
      })
    },
    handleSearch() {
      this.currentPage = 1
      this.loadUserList()
    },
    resetForm() {
      this.searchForm = {
        username: '',
        role: ''
      }
      this.currentPage = 1
      this.loadUserList()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.loadUserList()
    },
    handleCurrentChange(current) {
      this.currentPage = current
      this.loadUserList()
    },
    handleAdd() {
      this.dialogTitle = '新增用户'
      this.form = {
        id: '',
        username: '',
        password: '',
        nickname: '',
        phone: '',
        email: '',
        avatar: '',
        signature: '',
        role: 'user',
        status: 1
      }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑用户'
      this.form = Object.assign({}, row)
      this.form.password = ''
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
              await userApi.updateUser(submitData)
              this.$message.success('更新成功')
            } else {
              await userApi.addUser(submitData)
              this.$message.success('添加成功')
            }
            this.dialogVisible = false
            this.loadUserList()
          } catch (error) {
            console.error('操作失败:', error)
            this.$message.error('操作失败：' + (error.message || '未知错误'))
          }
        }
      })
    },
    handleDelete(id) {
      this.$confirm('确定要删除这个用户吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await userApi.deleteUser(id)
          this.$message.success('删除成功')
          this.loadUserList()
        } catch (error) {
          console.error('删除失败:', error)
        }
      })
    },
    handleStatusChange(row) {
      // 将status从布尔值转换为整数
      const submitData = Object.assign({}, row)
      submitData.status = row.status ? 1 : 0

      userApi.updateUser(submitData).then(() => {
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
.user-management {
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
