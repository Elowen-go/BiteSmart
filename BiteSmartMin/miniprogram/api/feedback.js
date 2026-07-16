"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createComplaint = exports.createReview = void 0;
const request_1 = require("../utils/request");
const createReview = (orderId, review) => (0, request_1.request)({ url: `/reviews?orderId=${orderId}`, method: 'POST', data: review });
exports.createReview = createReview;
const createComplaint = (payload) => (0, request_1.request)({ url: '/complaints', method: 'POST', data: payload });
exports.createComplaint = createComplaint;
