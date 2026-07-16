"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.clearAuth = exports.getUserInfo = exports.setAuth = exports.getToken = void 0;
const TOKEN_KEY = 'bitesmart_token';
const USER_KEY = 'bitesmart_user';
const normalizeUser = (stored) => {
    if (!stored)
        return null;
    if (typeof stored === 'string') {
        try {
            return normalizeUser(JSON.parse(stored));
        }
        catch (_) {
            return null;
        }
    }
    if (typeof stored !== 'object')
        return null;
    const value = stored;
    const role = value.role;
    const roleType = Number(value.roleType ?? role?.roleType ?? role?.type);
    if (![10, 20, 30].includes(roleType))
        return null;
    return { ...value, roleType };
};
const getToken = () => wx.getStorageSync(TOKEN_KEY) || '';
exports.getToken = getToken;
const setAuth = (token, user) => {
    const normalizedUser = normalizeUser(user);
    if (!token || !normalizedUser)
        throw new Error('登录响应缺少有效的用户信息');
    wx.setStorageSync(TOKEN_KEY, token);
    wx.setStorageSync(USER_KEY, normalizedUser);
};
exports.setAuth = setAuth;
const getUserInfo = () => normalizeUser(wx.getStorageSync(USER_KEY));
exports.getUserInfo = getUserInfo;
const clearAuth = () => {
    wx.removeStorageSync(TOKEN_KEY);
    wx.removeStorageSync(USER_KEY);
};
exports.clearAuth = clearAuth;
