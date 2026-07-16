import { getMembershipPlans, getMembershipStatus, type MembershipPlan } from '../../api/user'
import { requireUser } from '../../utils/user-route'
Page({ data: { plans: [] as MembershipPlan[], status: null as Record<string, unknown> | null }, onLoad() { if (!requireUser()) return; getMembershipPlans().then((plans) => this.setData({ plans })).catch(() => {}); getMembershipStatus().then((status) => this.setData({ status })).catch(() => {}) }, back() { wx.navigateBack() }, action(event: WechatMiniprogram.CustomEvent) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }) } })
