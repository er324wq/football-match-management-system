import Vue from 'vue'
import Router from 'vue-router'
import store from '../store'
import Login from '@/views/login/index'
import Layout from '@/layout/index'
import Frontend from '@/views/frontend/index'
import Dashboard from '@/views/dashboard/index'
import User from '@/views/user/index'
import Team from '@/views/team/index'
import Player from '@/views/player/index'
import Match from '@/views/match/index'
import MatchResult from '@/views/matchResult/index'
import GoalRecord from '@/views/goalRecord/index'
import MatchApplication from '@/views/matchApplication/index'
import Standings from '@/views/statistics/standings'
import TopScorers from '@/views/statistics/topScorers'
import Performance from '@/views/performance/index'
import Application from '@/views/admin/application/index'
// 前台页面组件
import FrontHome from '@/views/front/home/index'
import FrontMatch from '@/views/front/match/index'
import FrontTeam from '@/views/front/team/index'
import FrontPlayer from '@/views/front/player/index'
import FrontRanking from '@/views/front/ranking/index'
import FrontNotice from '@/views/front/notice/index'
import FrontUser from '@/views/front/user/index'
import FrontApply from '@/views/front/apply/index'
import FrontAthlete from '@/views/front/athlete/index'
import FrontReferee from '@/views/front/referee/index'
import FrontTeamManagement from '@/views/front/teamManagement/index'


Vue.use(Router)

const router = new Router({
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: Login,
      meta: { public: true, backend: true }
    },
    {
      path: '/frontend',
      name: 'Frontend',
      component: Frontend,
      meta: { public: true }
    },
    {
      path: '/',
      redirect: '/front/home',
      meta: { public: true }
    },

    // 前台页面路由
    {
      path: '/front/home',
      name: 'FrontHome',
      component: FrontHome,
      meta: { public: true }
    },
    {
      path: '/front/match',
      name: 'FrontMatch',
      component: FrontMatch,
      meta: { public: true }
    },
    {
      path: '/front/team',
      name: 'FrontTeam',
      component: FrontTeam,
      meta: { public: true }
    },
    {
      path: '/front/player',
      name: 'FrontPlayer',
      component: FrontPlayer,
      meta: { public: true }
    },
    {
      path: '/front/ranking',
      name: 'FrontRanking',
      component: FrontRanking,
      meta: { public: true }
    },
    {
      path: '/front/notice',
      name: 'FrontNotice',
      component: FrontNotice,
      meta: { public: true }
    },
    {
      path: '/front/user',
      name: 'FrontUser',
      component: FrontUser,
      meta: { public: true }
    },
    {
      path: '/front/apply',
      name: 'FrontApply',
      component: FrontApply,
      meta: { public: true }
    },
    {
      path: '/front/athlete',
      name: 'FrontAthlete',
      component: FrontAthlete,
      meta: { public: true }
    },
    {
      path: '/front/referee',
      name: 'FrontReferee',
      component: FrontReferee,
      meta: { public: true }
    },
    {
      path: '/front/team-management',
      name: 'FrontTeamManagement',
      component: FrontTeamManagement,
      meta: { public: true }
    },
    // 后台管理路由
    {
      path: '/admin',
      component: Layout,
      redirect: '/admin/dashboard',
      meta: { backend: true },
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: Dashboard,
          meta: { title: '首页', icon: 'el-icon-s-home', backend: true }
        },
        {
          path: 'user',
          name: 'User',
          component: User,
          meta: { title: '用户管理', icon: 'el-icon-user', backend: true }
        },
        {
          path: 'application',
          name: 'Application',
          component: Application,
          meta: { title: '申请管理', icon: 'el-icon-document', backend: true }
        },
        {
          path: 'team',
          name: 'Team',
          component: Team,
          meta: { title: '球队管理', icon: 'el-icon-football', backend: true }
        },
        {
          path: 'player',
          name: 'Player',
          component: Player,
          meta: { title: '球员管理', icon: 'el-icon-s-custom', backend: true }
        },
        {
          path: 'match',
          name: 'Match',
          component: Match,
          meta: { title: '比赛管理', icon: 'el-icon-date', backend: true }
        },
        {
          path: 'match-result',
          name: 'MatchResult',
          component: MatchResult,
          meta: { title: '比赛结果管理', icon: 'el-icon-finished', backend: true }
        },
        {
          path: 'goal-record',
          name: 'GoalRecord',
          component: GoalRecord,
          meta: { title: '比赛记录管理', icon: 'el-icon-s-flag', backend: true }
        },
        {
          path: 'match-application',
          name: 'MatchApplication',
          component: MatchApplication,
          meta: { title: '赛事申请管理', icon: 'el-icon-document', backend: true }
        },
        {
          path: 'standings',
          name: 'Standings',
          component: Standings,
          meta: { title: '积分榜', icon: 'el-icon-data-analysis', backend: true }
        },
        {
          path: 'top-scorers',
          name: 'TopScorers',
          component: TopScorers,
          meta: { title: '射手榜', icon: 'el-icon-trophy', backend: true }
        },
        {
          path: 'performance',
          name: 'Performance',
          component: Performance,
          meta: { title: '性能管理', icon: 'el-icon-data-analysis', backend: true }
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  // Clear a previous session when the user explicitly opens the login page.
  if (to.path === '/login') {
    store.commit('LOGOUT')
  }

  if (to.meta.backend && to.path !== '/login' && !store.getters.isLoggedIn) {
    next('/login')
    return
  }

  if (to.meta.backend && to.path !== '/login' && store.state.user.role !== 'admin') {
    next('/front/home')
    return
  }

  if (to.path === '/' && store.getters.isLoggedIn) {
    const user = store.state.user
    if (user && user.role === 'admin') {
      next('/admin')
    } else {
      next('/front/home')
    }
    return
  }

  next()
})

export default router
