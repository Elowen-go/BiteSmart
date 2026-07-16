import { getProfile } from '../../api/user'
import { requireUser } from '../../utils/user-route'

Page({
  data: { profile: { height: '168', weight: '60.0', goal: '提升健康水平', tags: ['少油少盐', '高蛋白', '不吃香菜'] }, loading: false },
  onLoad() { if (!requireUser()) return; this.setData({ loading: true }); getProfile().then((profile) => { if (profile) this.setData({ 'profile.height': String(profile.height || this.data.profile.height), 'profile.weight': String(profile.weight || this.data.profile.weight), 'profile.goal': profile.healthGoal || this.data.profile.goal }) }).catch(() => {}).finally(() => this.setData({ loading: false })) },
  back() { wx.navigateBack() },
  goAddress() { wx.navigateTo({ url: '/pages/address/address' }) },
  goNotice() { wx.navigateTo({ url: '/pages/notice/notice' }) },
  goSettings() { wx.navigateTo({ url: '/pages/settings/settings' }) },
  goComplaint() { wx.navigateTo({ url: '/pages/complaint/complaint' }) },
  action(event: WechatMiniprogram.CustomEvent) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }) }
})
