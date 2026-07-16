import { setupCredentials } from '../../api/auth'

Component({
  data: {
    username: '',
    phone: '',
    password: '',
    loading: false,
    errorMessage: ''
  },
  methods: {
    onInput(event: WechatMiniprogram.Input) {
      this.setData({ [event.currentTarget.dataset.field]: event.detail.value })
    },
    submit() {
      if (this.data.loading) return
      this.setData({ loading: true, errorMessage: '' })
      setupCredentials(this.data.username, this.data.phone, this.data.password)
        .then(() => wx.reLaunch({ url: '/pages/index/index' }))
        .catch((error: Error) => this.setData({ errorMessage: error.message || '保存失败' }))
        .finally(() => this.setData({ loading: false }))
    }
  }
})
