export const getToken = (): string | null => {
  return localStorage.getItem('token')
}

export const setToken = (token: string): void => {
  localStorage.setItem('token', token)
}

export const removeToken = (): void => {
  localStorage.removeItem('token')
}

export const getRole = (): string | null => {
  return localStorage.getItem('role')
}

export const setRole = (role: string): void => {
  localStorage.setItem('role', role)
}

export const removeRole = (): void => {
  localStorage.removeItem('role')
}

export const isAuthenticated = (): boolean => {
  return !!getToken()
}

export const isAdmin = (): boolean => {
  return getRole() === 'ADMIN' || getRole() === '40'
}

export const isMerchant = (): boolean => {
  return getRole() === 'MERCHANT' || getRole() === '20'
}

export const isUser = (): boolean => {
  return getRole() === 'USER' || getRole() === '10'
}

export const logout = (): void => {
  removeToken()
  removeRole()
  window.location.href = '/login'
}