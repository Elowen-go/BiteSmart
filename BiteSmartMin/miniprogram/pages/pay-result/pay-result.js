"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const user_route_1 = require("../../utils/user-route");
Page({ onLoad() { (0, user_route_1.requireUser)(); }, viewOrder() { wx.redirectTo({ url: '/pages/order/order' }); }, backHome() { wx.reLaunch({ url: '/pages/index/index' }); } });
