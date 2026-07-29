import type { Dish } from '../api/catalog'
import { API_ORIGIN } from './request'

export interface AiDishCard {
  id: number | string
  name: string
  image: string
  price: number
  calories: number
  protein: number
  stock: number
}

const normalizeDishImage = (value: unknown): string => {
  const image = String(value || '').trim()
  if (!image) return ''
  if (/^(https?:\/\/|data:image\/|wxfile:)/i.test(image)) return image
  if (image.startsWith('/uploads/')) return `${API_ORIGIN}/api/files/download/${image.slice('/uploads/'.length)}`
  if (image.startsWith('/')) return `${API_ORIGIN}${image}`
  return `${API_ORIGIN}/${image}`
}

export const toAiDishCard = (dish: Dish): AiDishCard | null => {
  if (dish.id == null || Number(dish.stock || 0) <= 0) return null
  return {
    id: dish.id,
    name: String(dish.dishName || '健康餐品'),
    image: normalizeDishImage(dish.dishImage),
    price: Number(dish.price || 0),
    calories: Number(dish.calories || 0),
    protein: Number(dish.protein || 0),
    stock: Number(dish.stock || 0)
  }
}

export const buildAiDishCards = (question: string, dishes: AiDishCard[]): AiDishCard[] => {
  const request = String(question || '').trim()
  if (!/推荐|吃什么|点什么|菜品|餐品|晚餐|午餐|早餐|加购|购物车|搭配|低卡|减脂|减重|轻食|控糖|高蛋白|增肌|健身/.test(request)) return []
  const explicit = dishes.filter((dish) => request.indexOf(dish.name) >= 0)
  if (explicit.length) return explicit.slice(0, 2)
  const lowCal = /低卡|减脂|减重|轻食|控糖/.test(request)
  const highProtein = /高蛋白|增肌|健身|蛋白质/.test(request)
  return dishes.slice().sort((a, b) => {
    if (highProtein) return b.protein - a.protein || a.calories - b.calories
    if (lowCal) return a.calories - b.calories || b.protein - a.protein
    return b.protein - a.protein || a.calories - b.calories
  }).slice(0, 2)
}
