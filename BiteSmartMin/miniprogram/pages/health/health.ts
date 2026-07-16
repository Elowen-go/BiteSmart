import { getWeightRecords, type WeightRecord } from '../../api/health'
import { requireUser } from '../../utils/user-route'

Page({
  data: { hasWeightHistory: false, weightRecords: [] as WeightRecord[] },
  onLoad() { if (!requireUser()) return; getWeightRecords().then((result) => { const records = Array.isArray(result) ? result : result.records; const recent = records.filter((record) => !record.recordDate || Date.now() - new Date(record.recordDate).getTime() <= 7 * 24 * 60 * 60 * 1000).slice(-7); this.setData({ weightRecords: recent, hasWeightHistory: recent.length >= 2 }) }).catch(() => {}) },
  back() { wx.navigateBack() },
  action(event: WechatMiniprogram.CustomEvent) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }) }
})
