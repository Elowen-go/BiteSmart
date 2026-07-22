import { recommend } from '../../api/ai'
import { addToCart } from '../../api/cart'
import { MOCK_DISHES, uimg } from '../../mock/catalog'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

const QUESTIONS = ['减脂晚餐怎么吃？', '今天蛋白质够吗？', '推荐一份低卡早餐', '帮我配一周餐单']

Page({
  data: {
    loading: false,
    image: uimg(MOCK_DISHES[0].img, 900),
    recommendation: {
      // 推荐菜品 id 为雪花字符串，原样透传禁止 Number() 强转
      id: MOCK_DISHES[0].id as number | string,
      name: MOCK_DISHES[0].name,
      calories: MOCK_DISHES[0].kcal,
      protein: MOCK_DISHES[0].protein,
      price: MOCK_DISHES[0].price,
      image: ''
    },
    questions: QUESTIONS,
    padTop: 0
  },
  onLoad() {
    if (!requireUser()) return
    const { padTop } = getSafeArea()
    this.setData({ loading: true, padTop })
    recommend().then((result) => {
      const item = (result.dish || result.recommendation || result) as Record<string, unknown>
      this.setData({
        recommendation: {
          id: (item.id || item.dishId || this.data.recommendation.id) as number | string,
          name: String(item.name || item.dishName || this.data.recommendation.name),
          calories: Number(item.calories || this.data.recommendation.calories),
          protein: Number(item.protein || this.data.recommendation.protein),
          price: Number(item.price || this.data.recommendation.price),
          image: String(item.image || this.data.image)
        }
      })
    }).catch(() => {}).finally(() => this.setData({ loading: false }))
  },
  openChat(event?: WechatMiniprogram.CustomEvent) {
    const question = event && event.currentTarget.dataset.question
    wx.navigateTo({ url: `/pages/chat/chat${question ? `?question=${encodeURIComponent(String(question))}` : ''}` })
  },
  addRecommendation() {
    addToCart(10, this.data.recommendation.id)
      .then(() => wx.showToast({ title: '已加入购物车', icon: 'none' }))
      .catch((error: Error) => wx.showToast({ title: error.message || '加入失败', icon: 'none' }))
  },
  openRecommendation() {
    wx.navigateTo({ url: `/pages/food-detail/food-detail?id=${this.data.recommendation.id}` })
  },
  goAssessment() { wx.navigateTo({ url: '/pages/assessment/assessment' }) }
})
