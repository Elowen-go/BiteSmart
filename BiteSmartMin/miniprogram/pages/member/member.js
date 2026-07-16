"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const user_1 = require("../../api/user");
const user_route_1 = require("../../utils/user-route");
Page({ data: { plans: [], status: null }, onLoad() { if (!(0, user_route_1.requireUser)())
        return; (0, user_1.getMembershipPlans)().then((plans) => this.setData({ plans })).catch(() => { }); (0, user_1.getMembershipStatus)().then((status) => this.setData({ status })).catch(() => { }); }, back() { wx.navigateBack(); }, action(event) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }); } });
