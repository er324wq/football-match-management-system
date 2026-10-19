import Vue from 'vue'
import Vuex from 'vuex'
import * as auth from '../api/auth'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user')) || null
  },
  getters: {
    token: state => state.token,
    user: state => state.user,
    isLoggedIn: state => !!state.user && !!state.token
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    SET_USER(state, user) {
      state.user = user
      localStorage.setItem('user', JSON.stringify(user))
    },
    LOGOUT(state) {
      state.token = ''
      state.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  },
  actions: {
    async login({ commit }, loginForm) {
      const data = await auth.login(loginForm)
      commit('SET_TOKEN', data.token)
      commit('SET_USER', data.user)
      return data
    },
    async logout({ commit }) {
      try {
        await auth.logout()
      } catch (error) {
        // Fallback: even if backend logout fails, clear local auth state.
      } finally {
        commit('LOGOUT')
      }
    }
  }
})
