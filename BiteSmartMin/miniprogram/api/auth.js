"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.bindWechat = exports.setupCredentials = exports.loginWithAccount = exports.loginWithWechat = void 0;
const auth_1 = require("../utils/auth");
const request_1 = require("../utils/request");
const loginWithWechat = (code, roleType) => {
    return (0, request_1.request)({
        url: '/auth/wechat-login',
        method: 'POST',
        data: { code, roleType },
        needAuth: false
    }).then((result) => {
        (0, auth_1.setAuth)(result.token, result.user);
        return result;
    });
};
exports.loginWithWechat = loginWithWechat;
const loginWithAccount = (username, password, roleType) => {
    return (0, request_1.request)({
        url: '/auth/login',
        method: 'POST',
        data: { username, password, roleType },
        needAuth: false
    }).then((result) => {
        (0, auth_1.setAuth)(result.token, result.user);
        return result;
    });
};
exports.loginWithAccount = loginWithAccount;
const setupCredentials = (username, phone, password) => {
    return (0, request_1.request)({
        url: '/auth/credentials',
        method: 'PUT',
        data: { username, phone, password }
    });
};
exports.setupCredentials = setupCredentials;
const bindWechat = (code) => {
    return (0, request_1.request)({
        url: '/auth/wechat-bind',
        method: 'POST',
        data: { code }
    });
};
exports.bindWechat = bindWechat;
