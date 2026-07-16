"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const cart_1 = require("../../api/cart");
const catalog_1 = require("../../api/catalog");
const user_route_1 = require("../../utils/user-route");
Page({ data: { combo: { id: 1, name: '7 天轻松减脂计划', desc: '高蛋白 · 控热量 · 每日三餐', price: '98', kcal: '1500 kcal/天', image: 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=900&q=80' } }, onLoad(options) { if (!(0, user_route_1.requireUser)())
        return; const id = Number(options?.id || 1); (0, catalog_1.getCombo)(id).then((result) => { const combo = (result.combo || result); this.setData({ combo: { ...this.data.combo, id, name: String(combo.name || combo.comboName || this.data.combo.name), desc: String(combo.description || this.data.combo.desc), price: String(combo.price || this.data.combo.price), kcal: `${combo.calories || 1500} kcal/天`, image: String(combo.image || this.data.combo.image) } }); }).catch(() => { }); }, back() { wx.navigateBack(); }, add() { (0, cart_1.addToCart)(20, this.data.combo.id).then(() => wx.showToast({ title: '套餐已加入购物车', icon: 'none' })).catch((error) => wx.showToast({ title: error.message || '加入失败', icon: 'none' })); } });
