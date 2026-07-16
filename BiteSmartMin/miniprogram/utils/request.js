"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.request = void 0;
const auth_1 = require("./auth");
const BASE_URL = 'http://localhost:8080/api';
const request = (options) => {
    return new Promise((resolve, reject) => {
        const token = (0, auth_1.getToken)();
        if (options.needAuth !== false && !token) {
            reject(new Error('请先登录'));
            return;
        }
        wx.request({
            url: `${BASE_URL}${options.url}`,
            method: options.method || 'GET',
            timeout: 8000,
            data: options.data,
            header: {
                'Content-Type': options.contentType === 'form' ? 'application/x-www-form-urlencoded' : 'application/json',
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            },
            success: (response) => {
                const result = response.data;
                if (!result || typeof result.code !== 'number') {
                    reject(new Error('服务响应异常，请稍后重试'));
                    return;
                }
                if (result.code >= 400) {
                    reject(new Error(result.message || '请求失败'));
                    return;
                }
                resolve(result.data);
            },
            fail: reject
        });
    });
};
exports.request = request;
