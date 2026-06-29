import request from '../utils/request'

/**
 * 用户注册 API
 * @param {Object} data { username, password, nickname, email }
 */
export function registerApi(data) {
  return request({
    url: '/api/user/register',
    method: 'post',
    data
  })
}

/**
 * 用户登录 API
 * @param {Object} data { username, password }
 */
export function loginApi(data) {
  return request({
    url: '/api/user/login',
    method: 'post',
    data
  })
}

/**
 * 获取当前登录用户信息 API
 */
export function getUserInfoApi() {
  return request({
    url: '/api/user/info',
    method: 'get'
  })
}
