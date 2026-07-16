"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const auth_1 = require("../../utils/auth");
const user_route_1 = require("../../utils/user-route");
Page({ onLoad() { (0, user_route_1.requireUser)(); }, back() { wx.navigateBack(); }, action(event) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }); }, logout() { (0, auth_1.clearAuth)(); wx.reLaunch({ url: '/pages/login/login' }); } });
