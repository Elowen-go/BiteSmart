const PROFILE_KEYS = [
  'age',
  'gender',
  'height',
  'weight',
  'activityLevel',
  'activity_level',
  'dietPreference',
  'diet_preference',
  'allergyInfo',
  'allergy_info',
  'diseaseHistory',
  'disease_history',
  'healthGoal',
  'health_goal'
]

const isObject = (value: unknown): value is Record<string, any> => {
  return !!value && typeof value === 'object' && !Array.isArray(value)
}

const looksLikeProfile = (value: Record<string, any>) => {
  return PROFILE_KEYS.some(key => Object.prototype.hasOwnProperty.call(value, key))
}

const unwrap = (value: any, depth = 0): Record<string, any> => {
  if (!isObject(value) || depth > 4) return {}
  if (looksLikeProfile(value)) return value

  for (const key of ['data', 'profile', 'result']) {
    const nested = unwrap(value[key], depth + 1)
    if (Object.keys(nested).length) return nested
  }
  return {}
}

const numberOrNull = (value: any) => {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : value
}

export const extractProfile = (response: any) => {
  const source = unwrap(response)
  return {
    age: numberOrNull(source.age),
    gender: numberOrNull(source.gender ?? source.gender_code),
    height: numberOrNull(source.height),
    weight: numberOrNull(source.weight),
    activityLevel: numberOrNull(source.activityLevel ?? source.activity_level),
    dietPreference: source.dietPreference ?? source.diet_preference ?? '',
    allergyInfo: source.allergyInfo ?? source.allergy_info ?? '',
    diseaseHistory: source.diseaseHistory ?? source.disease_history ?? '',
    healthGoal: source.healthGoal ?? source.health_goal ?? ''
  }
}
