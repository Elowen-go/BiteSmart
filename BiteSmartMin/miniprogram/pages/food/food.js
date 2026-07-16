"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const cart_1 = require("../../api/cart");
const catalog_1 = require("../../api/catalog");
const user_route_1 = require("../../utils/user-route");
const fallback = [
    { id: 1, name: '青柠香煎鸡胸', desc: '高蛋白 · 低脂 · 现做', price: '29', kcal: '426 kcal', image: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=700&q=80' },
    { id: 2, name: '照烧三文鱼谷物碗', desc: '优质脂肪 · 饱腹组合', price: '36', kcal: '508 kcal', image: 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=700&q=80' },
    { id: 3, name: '南瓜藜麦暖沙拉', desc: '膳食纤维 · 当季蔬菜', price: '25', kcal: '318 kcal', image: 'https://images.unsplash.com/photo-1516684669134-de6f7c473a2a?w=700&q=80' }
];
Page({
    data: { cartCount: 0, items: fallback, topStyle: '' },
    onLoad() { if (!(0, user_route_1.requireUser)())
        return; const systemInfo = wx.getSystemInfoSync(); const menuButton = wx.getMenuButtonBoundingClientRect(); const topHeight = Math.max(menuButton.bottom + 12, systemInfo.statusBarHeight + 56); this.setData({ topStyle: `padding-top:${topHeight}px;` }); (0, catalog_1.getDishes)().then((result) => { const items = Array.isArray(result) ? result : result.records; if (items.length)
        this.setData({ items: items.map((item, index) => ({ id: item.id || index + 1, name: item.name || item.dishName || '健康菜品', desc: item.description || '营养搭配 · 现做', price: String(item.price || 0), kcal: `${item.calories || 0} kcal`, image: item.image || fallback[index % fallback.length].image })) }); }).catch(() => { }); },
    back() { wx.navigateBack(); },
    openCart() { wx.navigateTo({ url: '/pages/cart/cart' }); },
    openCombos() { wx.navigateTo({ url: '/pages/combo/combo' }); },
    search() { },
    openDetail(event) { wx.navigateTo({ url: `/pages/food-detail/food-detail?index=${event.currentTarget.dataset.index}` }); },
    addDish(event) { const item = this.data.items[Number(event.currentTarget.dataset.index)]; (0, cart_1.addToCart)(10, Number(item.id || 1)).then(() => wx.showToast({ title: '已加入购物车', icon: 'none' })).catch((error) => wx.showToast({ title: error.message || '加入失败', icon: 'none' })); }
});
