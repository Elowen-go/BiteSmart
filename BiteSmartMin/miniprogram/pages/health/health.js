"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const health_1 = require("../../api/health");
const user_route_1 = require("../../utils/user-route");
Page({
    data: { hasWeightHistory: false, weightRecords: [] },
    onLoad() { if (!(0, user_route_1.requireUser)())
        return; (0, health_1.getWeightRecords)().then((result) => { const records = Array.isArray(result) ? result : result.records; const recent = records.filter((record) => !record.recordDate || Date.now() - new Date(record.recordDate).getTime() <= 7 * 24 * 60 * 60 * 1000).slice(-7); this.setData({ weightRecords: recent, hasWeightHistory: recent.length >= 2 }); }).catch(() => { }); },
    back() { wx.navigateBack(); },
    action(event) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }); }
});
