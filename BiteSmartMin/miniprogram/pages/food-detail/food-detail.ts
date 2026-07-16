import { requireUser } from '../../utils/user-route'
import { addToCart } from '../../api/cart'
Page({ data: { item: { id: 1, name: '青柠香煎鸡胸', desc: '高蛋白 · 低脂 · 现做', price: '29', kcal: '426 kcal', image: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=900&q=80' } }, onLoad() { requireUser() }, back() { wx.navigateBack() }, add() { addToCart(10, this.data.item.id).then(() => wx.showToast({ title: '已加入购物车', icon: 'none' })).catch((error: Error) => wx.showToast({ title: error.message || '加入失败', icon: 'none' })) } })
