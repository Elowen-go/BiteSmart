import { buyMembership, getMembershipPlans, getMembershipStatus, type MembershipPlan } from '../../api/user'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

interface PlanView { id: number | string; name: string; price: string; org: string; days: string; best: boolean }

const BENEFITS = [
  { ic: '免', t: '全场免配送费', s: '会员期间外卖订单免配送费' },
  { ic: '折', t: '会员专享折扣', s: '套餐与单品均可叠加使用' },
  { ic: 'AI', t: 'AI 私人定制餐单', s: '每周一份专属饮食计划' },
  { ic: '积', t: '生日双倍积分', s: '积分可兑换招牌菜品' }
]

const MEMBER_TYPE: Record<number, string> = { 10: '月卡', 20: '季卡', 30: '年卡' }

const toPlanView = (plan: MembershipPlan, index: number, all: MembershipPlan[]): PlanView => ({
  id: plan.id,
  name: plan.planName || 'PRO 会员',
  price: plan.price != null ? String(plan.price) : '--',
  org: plan.originalPrice != null ? String(plan.originalPrice) : '',
  days: plan.validDays ? `${plan.validDays} 天` : '',
  best: all.length > 1 && index === 1
})

Page({
  data: {
    plans: [] as PlanView[],
    isVip: false,
    expire: '',
    typeText: '',
    benefits: BENEFITS,
    buying: false,
    menuTop: 0,
    menuH: 32
  },

  onLoad() {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH })
    getMembershipPlans()
      .then((plans) => this.setData({ plans: plans.map(toPlanView) }))
      .catch(() => {})
    this.refreshStatus()
  },

  refreshStatus() {
    getMembershipStatus()
      .then((status) => this.setData({
        isVip: Boolean(status && status.status === 10),
        expire: status && status.endTime ? String(status.endTime).slice(0, 10) : '',
        typeText: status && status.membershipType ? MEMBER_TYPE[status.membershipType] || '' : ''
      }))
      .catch(() => {})
  },

  back() { wx.navigateBack() },

  buy(event: WechatMiniprogram.CustomEvent) {
    const id = String(event.currentTarget.dataset.id || '')
    if (!id || this.data.buying) return
    const plan = this.data.plans.find((item) => String(item.id) === id)
    if (!plan) return
    wx.showModal({
      title: '确认开通会员',
      content: `${plan.name} · ${plan.days}，金额 ¥${plan.price}`,
      confirmText: '确认购买',
      cancelText: '再看看',
      success: (result) => {
        if (!result.confirm) return
        this.setData({ buying: true })
        buyMembership(id)
          .then((payment) => {
            if (payment && payment.paymentMode === 'alipay-sandbox') {
              wx.showModal({
                title: '请使用电脑完成支付',
                content: '当前支付宝沙箱采用电脑网页支付，请在 PC 端会员中心完成支付。',
                showCancel: false
              })
              return
            }
            throw new Error('支付宝支付订单创建失败')
          })
          .catch((error: Error) => wx.showToast({ title: error.message || '购买失败', icon: 'none' }))
          .finally(() => this.setData({ buying: false }))
      }
    })
  }
})
