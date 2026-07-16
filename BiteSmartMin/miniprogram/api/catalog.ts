import { request } from '../utils/request'

export interface Dish { id?: number; name?: string; dishName?: string; image?: string; price?: number; calories?: number; protein?: number; fat?: number; carbohydrate?: number; description?: string }
export interface Combo { id?: number; name?: string; comboName?: string; image?: string; price?: number; calories?: number; description?: string }
export const getDishes = (params: Record<string, unknown> = {}): Promise<Dish[] | { records: Dish[] }> => request<Dish[] | { records: Dish[] }>({ url: '/dishes', data: params, needAuth: false })
export const getDish = (id: number): Promise<Dish> => request<Dish>({ url: `/dishes/${id}`, needAuth: false })
export const getCombos = (params: Record<string, unknown> = {}): Promise<Combo[]> => request<Combo[]>({ url: '/combos', data: params, needAuth: false })
export const getCombo = (id: number): Promise<Record<string, unknown>> => request<Record<string, unknown>>({ url: `/combos/${id}`, needAuth: false })
