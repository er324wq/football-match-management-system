<template>
  <div class="standings">
    <el-card>
      <template slot="header">
        <div class="card-header">
          <span>积分榜</span>
        </div>
      </template>

      <el-table :data="standingsList" style="width: 100%">
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
        <el-table-column prop="teamName" label="球队"></el-table-column>
        <el-table-column prop="played" label="场次" width="80"></el-table-column>
        <el-table-column prop="won" label="胜" width="80"></el-table-column>
        <el-table-column prop="drawn" label="平" width="80"></el-table-column>
        <el-table-column prop="lost" label="负" width="80"></el-table-column>
        <el-table-column prop="goalsFor" label="进球" width="80"></el-table-column>
        <el-table-column prop="goalsAgainst" label="失球" width="80"></el-table-column>
        <el-table-column prop="goalDifference" label="净胜球" width="100"></el-table-column>
        <el-table-column prop="points" label="积分" width="80">
          <template slot-scope="scope">
            <span style="font-weight: bold;">{{ scope.row.points }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import * as statisticsApi from '@/api/statistics'

export default {
  name: 'Standings',
  data() {
    return {
      standingsList: []
    }
  },
  mounted() {
    this.loadStandings()
  },
  methods: {
    loadStandings() {
      statisticsApi.getStandings().then(res => {
        this.standingsList = res
      })
    }
  }
}
</script>

<style scoped>
.standings {
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
