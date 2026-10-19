<template>
  <div class="admin-application">
    <div class="page-header">
      <h1>申请管理</h1>
    </div>

    <el-tabs v-model="activeTab" @tab-click="loadApplications">
      <el-tab-pane label="待处理申请" name="pending">
        <el-table :data="pendingApplications" style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="类型" width="120">
            <template slot-scope="scope">
              <el-tag :type="scope.row.type === 'team' ? 'primary' : 'info'">
                {{ getTypeLabel(scope.row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetName" label="目标" min-width="180" show-overflow-tooltip />
          <el-table-column prop="userName" label="申请人" width="130" />
          <el-table-column prop="reasonText" label="申请理由" min-width="220" show-overflow-tooltip />
          <el-table-column prop="createTime" label="申请时间" width="180" />
          <el-table-column label="操作" width="250">
            <template slot-scope="scope">
              <el-button size="mini" @click="viewApplication(scope.row)">查看</el-button>
              <el-button type="primary" size="mini" @click="updateStatus(scope.row.id, 'approved')">同意</el-button>
              <el-button type="danger" size="mini" @click="updateStatus(scope.row.id, 'rejected')">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="历史记录" name="history">
        <el-table :data="historyApplications" style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="类型" width="120">
            <template slot-scope="scope">
              <el-tag :type="scope.row.type === 'team' ? 'primary' : 'info'">
                {{ getTypeLabel(scope.row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetName" label="目标" min-width="180" show-overflow-tooltip />
          <el-table-column prop="userName" label="申请人" width="130" />
          <el-table-column prop="reasonText" label="申请理由" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'approved' ? 'success' : 'danger'">
                {{ scope.row.status === 'approved' ? '已同意' : '已拒绝' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="180" />
          <el-table-column prop="updateTime" label="处理时间" width="180" />
          <el-table-column label="操作" width="90">
            <template slot-scope="scope">
              <el-button size="mini" @click="viewApplication(scope.row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog :visible.sync="viewDialogVisible" title="申请详情" width="560px">
      <el-form :model="currentApplication" label-width="100px">
        <el-form-item label="申请ID">{{ currentApplication.id }}</el-form-item>
        <el-form-item label="申请类型">{{ getTypeLabel(currentApplication.type) }}</el-form-item>
        <el-form-item label="目标">{{ currentApplication.targetName }}</el-form-item>
        <el-form-item label="申请人">{{ currentApplication.userName }}</el-form-item>
        <el-form-item label="申请理由">{{ currentApplication.reasonText }}</el-form-item>
        <el-form-item label="申请时间">{{ currentApplication.createTime }}</el-form-item>
        <el-form-item label="状态">
          <el-tag :type="currentApplication.status === 'approved' ? 'success' : currentApplication.status === 'rejected' ? 'danger' : 'info'">
            {{ currentApplication.status === 'approved' ? '已同意' : currentApplication.status === 'rejected' ? '已拒绝' : '待处理' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="处理时间" v-if="currentApplication.updateTime">{{ currentApplication.updateTime }}</el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>
import * as applicationApi from '@/api/application'

export default {
  name: 'AdminApplication',
  data() {
    return {
      activeTab: 'pending',
      pendingApplications: [],
      historyApplications: [],
      viewDialogVisible: false,
      currentApplication: {}
    }
  },
  mounted() {
    this.loadApplications()
  },
  methods: {
    getTypeLabel(type) {
      return type === 'team' ? '加入球队' : '创建球队'
    },
    normalizeApp(app) {
      const targetName = app.type === 'team'
        ? `球队ID: ${app.targetId || '-'}`
        : (app.name || `申请ID: ${app.id}`)
      return {
        ...app,
        targetName,
        userName: `用户ID: ${app.userId || '-'}`,
        reasonText: app.reason || app.description || '-'
      }
    },
    async loadApplications() {
      try {
        const all = await applicationApi.getAllApplications()
        const teamOnly = (all || [])
          .filter(app => app.type === 'team' || app.type === 'create_team')
          .map(this.normalizeApp)
        this.pendingApplications = teamOnly.filter(app => app.status === 'pending')
        this.historyApplications = teamOnly.filter(app => app.status !== 'pending')
      } catch (e) {
        this.pendingApplications = []
        this.historyApplications = []
        this.$message.error('获取申请列表失败')
      }
    },
    async updateStatus(id, status) {
      try {
        await applicationApi.updateApplicationStatus(id, status)
        this.$message.success(status === 'approved' ? '已同意申请' : '已拒绝申请')
        this.loadApplications()
      } catch (e) {
        this.$message.error(status === 'approved' ? '同意申请失败' : '拒绝申请失败')
      }
    },
    viewApplication(row) {
      this.currentApplication = row || {}
      this.viewDialogVisible = true
    }
  }
}
</script>

<style scoped>
.admin-application {
  padding: 20px;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
}
</style>
