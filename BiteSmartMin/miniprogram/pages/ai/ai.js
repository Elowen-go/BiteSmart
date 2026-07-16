"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const user_route_1 = require("../../utils/user-route");
Page({ data: { image: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=900&q=80' }, onLoad() { (0, user_route_1.requireUser)(); }, back() { wx.navigateBack(); }, openChat() { wx.navigateTo({ url: '/pages/chat/chat' }); }, action(event) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }); } });
