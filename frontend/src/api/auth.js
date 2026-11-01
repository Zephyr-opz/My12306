import request from '@/utils/request'

export const login = (data) => {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export const register = (data) => {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export const getCurrentUser = () => {
  return request({
    url: '/users/current',
    method: 'get'
  })
}

export const changePassword = (data) => {
  return request({
    url: '/users/password',
    method: 'post',
    data
  })
}

export const sendResetCode = (data) => {
  return request({
    url: '/auth/password/reset/code',
    method: 'post',
    data
  })
}

export const resetPassword = (data) => {
  return request({
    url: '/auth/password/reset',
    method: 'post',
    data
  })
}
