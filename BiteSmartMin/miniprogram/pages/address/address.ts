import { getAddresses, type UserAddress } from '../../api/user'
import { requireUser } from '../../utils/user-route'
Page({ data: { addresses: [] as UserAddress[], loading: false }, onLoad() { if (!requireUser()) return; this.setData({ loading: true }); getAddresses().then((addresses) => this.setData({ addresses })).catch(() => {}).finally(() => this.setData({ loading: false })) }, back() { wx.navigateBack() }, action(event: WechatMiniprogram.CustomEvent) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }) } })
