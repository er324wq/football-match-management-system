<template>
  <div class="top-scorers">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>射手榜</span>
        </div>
      </template>

      <el-table :data="topScorersList" style="width: 100%">
        <el-table-column label="排名" width="80" align="center">
          <template slot-scope="scope">
            <div class="rank-container">
              <div v-if="scope.$index === 0" class="medal gold">
                <span>{{ scope.$index + 1 }}</span>
              </div>
              <div v-else-if="scope.$index === 1" class="medal silver">
                <span>{{ scope.$index + 1 }}</span>
              </div>
              <div v-else-if="scope.$index === 2" class="medal bronze">
                <span>{{ scope.$index + 1 }}</span>
              </div>
              <span v-else class="rank-number">{{ scope.$index + 1 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="playerName" label="球员"></el-table-column>
        <el-table-column prop="teamName" label="所属球队"></el-table-column>
        <el-table-column prop="goalCount" label="进球数" width="100">
          <template slot-scope="scope">
            <span style="font-weight: bold;">{{ scope.row.goalCount }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import * as statisticsApi from '@/api/statistics'

export default {
  name: 'TopScorers',
  data() {
    return {
      topScorersList: []
    }
  },
  mounted() {
    this.loadTopScorers()
  },
  methods: {
    loadTopScorers() {
      statisticsApi.getTopScorers().then(res => {
        this.topScorersList = res
      })
    }
  }
}
</script>

<style scoped>
.top-scorers {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rank-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
}

.medal {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  font-weight: bold;
  color: white;
}

.rank-number {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 30px;
  height: 30px;
}

.medal.gold {
  background-color: #FFD700;
  box-shadow: 0 0 5px rgba(255, 215, 0, 0.5);
}

.medal.silver {
  background-color: #C0C0C0;
  box-shadow: 0 0 5px rgba(192, 192, 192, 0.5);
}

.medal.bronze {
  background-color: #CD7F32;
  box-shadow: 0 0 5px rgba(205, 127, 50, 0.5);
}
</style>
