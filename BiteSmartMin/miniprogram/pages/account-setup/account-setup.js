"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const auth_1 = require("../../api/auth");
Component({
    data: {
        username: '',
        phone: '',
        password: '',
        loading: false,
        errorMessage: ''
    },
    methods: {
        onInput(event) {
            this.setData({ [event.currentTarget.dataset.field]: event.detail.value });
        },
        submit() {
            if (this.data.loading)
                return;
            this.setData({ loading: true, errorMessage: '' });
            (0, auth_1.setupCredentials)(this.data.username, this.data.phone, this.data.password)
                .then(() => wx.reLaunch({ url: '/pages/index/index' }))
                .catch((error) => this.setData({ errorMessage: error.message || '保存失败' }))
                .finally(() => this.setData({ loading: false }));
        }
    }
});
