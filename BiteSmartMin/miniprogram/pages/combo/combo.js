"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const catalog_1 = require("../../api/catalog");
const user_route_1 = require("../../utils/user-route");
const fallback = [{ id: 1, name: '7 天轻松减脂计划', desc: '高蛋白 · 控热量 · 每日三餐', price: '98', days: '7', kcal: '1500 kcal/天', image: 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=900&q=80' }, { id: 2, name: '学生党均衡食谱', desc: '饱腹好吃 · 轻松坚持 · 适合忙碌日常', price: '128', days: '7', kcal: '1800 kcal/天', image: 'https://images.unsplash.com/photo-1547592180-85f173990554?w=900&q=80' }];
Page({ data: { items: fallback }, onLoad() { if (!(0, user_route_1.requireUser)())
        return; (0, catalog_1.getCombos)().then((items) => { if (items.length)
        this.setData({ items: items.map((item) => ({ id: item.id || 0, name: item.name || item.comboName || '健康套餐', desc: item.description || '营养搭配 · 目标定制', price: String(item.price || 0), days: '7', kcal: `${item.calories || 0} kcal/天`, image: item.image || fallback[0].image })) }); }).catch(() => { }); }, back() { wx.navigateBack(); }, openDetail(event) { wx.navigateTo({ url: `/pages/combo-detail/combo-detail?id=${event.currentTarget.dataset.id}` }); } });
