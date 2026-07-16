"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const user_1 = require("../../api/user");
const user_route_1 = require("../../utils/user-route");
Page({ data: { addresses: [], loading: false }, onLoad() { if (!(0, user_route_1.requireUser)())
        return; this.setData({ loading: true }); (0, user_1.getAddresses)().then((addresses) => this.setData({ addresses })).catch(() => { }).finally(() => this.setData({ loading: false })); }, back() { wx.navigateBack(); }, action(event) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }); } });
