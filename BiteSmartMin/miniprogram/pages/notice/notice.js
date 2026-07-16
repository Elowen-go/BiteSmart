"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const user_route_1 = require("../../utils/user-route");
Page({ onLoad() { (0, user_route_1.requireUser)(); }, back() { wx.navigateBack(); } });
