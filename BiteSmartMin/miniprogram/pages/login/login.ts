import { loginWithAccount, loginWithWechat } from '../../api/auth'
import { getToken, getUserInfo } from '../../utils/auth'
import { getSafeArea } from '../../utils/safe-area'

/** 按角色分流首页：10 用户 → 用户端首页；20 商家 → 商家端工作台；30 骑手 → 骑手端（占位建设中） */
const homeByRole = (roleType: number): string => {
  if (roleType === 20) return '/pages/m-home/m-home'
  if (roleType === 30) return '/pages/r-tasks/r-tasks'
  return '/pages/index/index'
}

Component({
  data: {
    loading: false,
    errorMessage: '',
    roleType: 10,
    roleName: 'User',
    mode: 'wechat',
    username: '',
    password: '',
    padTop: 0
  },
  lifetimes: {
    attached() {
      const { padTop } = getSafeArea()
      this.setData({ padTop })
      const user = getUserInfo()
      if (getToken() && user && [10, 20, 30].includes(Number(user.roleType))) {
        wx.reLaunch({ url: homeByRole(Number(user.roleType)) })
      }
    }
  },
  methods: {
    enterApp(url: string) {
      const showNavigationError = () => this.setData({ loading: false, errorMessage: '登录成功，但页面跳转失败，请重新编译小程序' })
      wx.redirectTo({
        url,
        fail: () => {
          wx.navigateTo({
            url,
            fail: () => {
              wx.reLaunch({ url, fail: showNavigationError })
            }
          })
        }
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
          .then(() => this.enterApp(homeByRole(this.data.roleType)))
          .catch((error: Error) => this.setData({ errorMessage: error.message || '登录失败，请重试' }))
          .finally(() => this.setData({ loading: false }))
        return
      }
      wx.login({
        success: (result) => {
          loginWithWechat(result.code, this.data.roleType)
            .then((response) => {
              // 账号补全页是用户端流程；商家/骑手微信登录直接进各自端
              const url = response.accountSetupRequired && this.data.roleType === 10
                ? '/pages/account-setup/account-setup'
                : homeByRole(this.data.roleType)
              this.enterApp(url)
            })
            .catch((error: Error) => this.setData({ errorMessage: error.message || '登录失败，请重试' }))
            .finally(() => this.setData({ loading: false }))
        },
        fail: () => this.setData({ loading: false, errorMessage: '无法获取微信登录凭证' })
      })
    }
  }
})
