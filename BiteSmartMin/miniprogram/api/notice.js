"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getNotices = void 0;
const request_1 = require("../utils/request");
const getNotices = () => (0, request_1.request)({ url: '/notices', needAuth: false });
exports.getNotices = getNotices;
