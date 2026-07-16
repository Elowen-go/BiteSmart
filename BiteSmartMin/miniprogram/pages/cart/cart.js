"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const cart_1 = require("../../api/cart");
const user_route_1 = require("../../utils/user-route");
Page({
    data: { items: [], total: 54, topStyle: '' },
    onLoad() {
        if (!(0, user_route_1.requireUser)())
            return;
        const systemInfo = wx.getSystemInfoSync();
        const menuButton = wx.getMenuButtonBoundingClientRect();
        const topHeight = Math.max(menuButton.bottom + 12, systemInfo.statusBarHeight + 56);
        this.setData({ topStyle: `padding-top:${topHeight}px;` });
        (0, cart_1.getCart)().then((items) => this.setData({ items, total: items.reduce((sum, item) => sum + Number(item.price || 0) * item.quantity, 0) })).catch(() => { });
    },
    back() { wx.navigateBack(); },
    checkout() {
        wx.navigateTo({ url: '/pages/checkout/checkout' });
    }
});
