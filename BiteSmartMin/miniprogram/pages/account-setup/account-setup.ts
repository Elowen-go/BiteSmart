import { setupCredentials } from '../../api/auth'
import { getSafeArea } from '../../utils/safe-area'

Component({
  data: {
    username: '',
    phone: '',
    password: '',
    loading: false,
    errorMessage: '',
    padTop: 0
  },
  lifetimes: {
    attached() {
      const { padTop } = getSafeArea()
      this.setData({ padTop })
    }
  },
  methods: {
    onInput(event: WechatMiniprogram.Input) {
      this.setData({ [event.currentTarget.dataset.field]: event.detail.value })
    },
    submit() {
      if (this.data.loading) return
      if (!this.data.username || !this.data.phone || !this.data.password) {
        this.setData({ errorMessage: '请填写完整信息' })
        return
      }
      this.setData({ loading: true, errorMessage: '' })
      setupCredentials(this.data.username, this.data.phone, this.data.password)
        .then(() => wx.reLaunch({ url: '/pages/index/index' }))
        .catch((error: Error) => this.setData({ errorMessage: error.message || '保存失败' }))
        .finally(() => this.setData({ loading: false }))
    }
  }
})
