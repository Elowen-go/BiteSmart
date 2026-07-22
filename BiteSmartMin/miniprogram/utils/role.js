"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getRoleConfig = exports.ROLE_CONFIG = void 0;
exports.ROLE_CONFIG = {
    10: { type: 10, name: '健康用户', shortName: '用户端', greeting: '今天也要照顾好自己', accent: '#6F6DEB', tabs: ['首页', '商城', 'AI助手', '我的'] },
    20: { type: 20, name: '商家工作台', shortName: '商家端', greeting: '把每一份餐做好', accent: '#D58A38', tabs: ['工作台', '订单', '菜品', '我的'] },
    30: { type: 30, name: '配送工作台', shortName: '配送端', greeting: '准时，把热度送到', accent: '#3C7A8B', tabs: ['任务', '历史', '收益', '我的'] }
};
const getRoleConfig = (roleType) => {
    const normalizedRoleType = Number(roleType);
    if (normalizedRoleType !== 10 && normalizedRoleType !== 20 && normalizedRoleType !== 30)
        return null;
    return exports.ROLE_CONFIG[normalizedRoleType];
};
exports.getRoleConfig = getRoleConfig;
