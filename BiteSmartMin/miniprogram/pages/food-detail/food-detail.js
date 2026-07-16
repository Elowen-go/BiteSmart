"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const user_route_1 = require("../../utils/user-route");
const cart_1 = require("../../api/cart");
Page({ data: { item: { id: 1, name: '青柠香煎鸡胸', desc: '高蛋白 · 低脂 · 现做', price: '29', kcal: '426 kcal', image: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=900&q=80' } }, onLoad() { (0, user_route_1.requireUser)(); }, back() { wx.navigateBack(); }, add() { (0, cart_1.addToCart)(10, this.data.item.id).then(() => wx.showToast({ title: '已加入购物车', icon: 'none' })).catch((error) => wx.showToast({ title: error.message || '加入失败', icon: 'none' })); } });
