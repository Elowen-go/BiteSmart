"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const ai_1 = require("../../api/ai");
const user_route_1 = require("../../utils/user-route");
Page({
    data: { question: '', loading: false },
    onLoad() { (0, user_route_1.requireUser)(); },
    back() { wx.navigateBack(); },
    onInput(event) { this.setData({ question: event.detail.value }); },
    send() { if (!this.data.question || this.data.loading)
        return; this.setData({ loading: true }); (0, ai_1.chat)(this.data.question).then(() => this.setData({ question: '' })).catch(() => wx.showToast({ title: 'AI 暂时没有回应', icon: 'none' })).finally(() => this.setData({ loading: false })); },
    action(event) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }); }
});
