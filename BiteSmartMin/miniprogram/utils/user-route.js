"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.goUserPage = exports.requireUser = void 0;
const auth_1 = require("./auth");
const requireUser = () => {
    const user = (0, auth_1.getUserInfo)();
    if (!user || Number(user.roleType) !== 10) {
        wx.reLaunch({ url: '/pages/login/login' });
        return false;
    }
    return true;
};
exports.requireUser = requireUser;
const goUserPage = (url) => {
    if ((0, exports.requireUser)())
        wx.navigateTo({ url });
};
exports.goUserPage = goUserPage;
