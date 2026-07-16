import { loginWithAccount, loginWithWechat } from '../../api/auth'
import { getToken, getUserInfo } from '../../utils/auth'

Component({
  data: {
    loading: false,
    errorMessage: '',
    roleType: 10,
    roleName: 'User',
    mode: 'wechat',
    username: '',
    password: ''
  },
  lifetimes: {
    attached() {
      const user = getUserInfo()
      if (getToken() && user && [10, 20, 30].includes(Number(user.roleType))) {
        wx.reLaunch({ url: '/pages/index/index' })
      }
    }
  },
  methods: {
    enterApp(url: string) {
      wx.reLaunch({
        url,
        fail: () => this.setData({ loading: false, errorMessage: '登录成功，但页面跳转失败，请重新编译小程序' })
      })
    },
    selectRole(event: WechatMiniprogram.CustomEvent) {
      const roleType = Number(event.currentTarget.dataset.role)
      const roleName = roleType === 20 ? 'Merchant' : roleType === 30 ? 'Delivery' : 'User'
      this.setData({ roleType, roleName, errorMessage: '' })
    },
    switchMode() {
      this.setData({ mode: this.data.mode === 'wechat' ? 'account' : 'wechat', errorMessage: '' })
    },
    onUsernameInput(event: WechatMiniprogram.Input) {
      this.setData({ username: event.detail.value })
    },
    onPasswordInput(event: WechatMiniprogram.Input) {
      this.setData({ password: event.detail.value })
    },
    login() {
      if (this.data.loading) return
      this.setData({ loading: true, errorMessage: '' })
      if (this.data.mode === 'account') {
        loginWithAccount(this.data.username, this.data.password, this.data.roleType)
          .then(() => this.enterApp('/pages/index/index'))
          .catch((error: Error) => this.setData({ errorMessage: error.message || 'Login failed' }))
          .finally(() => this.setData({ loading: false }))
        return
      }
      wx.login({
        success: (result) => {
            loginWithWechat(result.code, this.data.roleType)
            .then((response) => {
              this.enterApp(response.accountSetupRequired ? '/pages/account-setup/account-setup' : '/pages/index/index')
            })
            .catch((error: Error) => this.setData({ errorMessage: error.message || 'Login failed' }))
            .finally(() => this.setData({ loading: false }))
        },
        fail: () => this.setData({ loading: false, errorMessage: 'Unable to get WeChat login code' })
      })
    }
  }
})
