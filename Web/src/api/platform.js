import request from '../utils/request'

/**
 * 获取支持的刷课平台列表及绑定状态
 */
export function getPlatformListApi() {
  return request({
    url: '/api/platform/list',
    method: 'get'
  })
}

/**
 * 获取用户在特定平台的绑定账号设置
 * @param {String} platformCode 平台代码（如 chaoxing）
 */
export function getPlatformConfigApi(platformCode) {
  return request({
    url: `/api/platform/config/${platformCode}`,
    method: 'get'
  })
}

/**
 * 绑定或更新网课平台账号密码
 * @param {Object} data { platformCode, username, password }
 */
export function savePlatformConfigApi(data) {
  return request({
    url: '/api/platform/config',
    method: 'post',
    data
  })
}

/**
 * 获取用户在指定平台的课程列表
 * @param {String} platformCode 平台代码（如 chaoxing）
 */
export function getCourseListApi(platformCode) {
  return request({
    url: `/api/platform/courses/${platformCode}`,
    method: 'get'
  })
}

/**
 * 解绑平台账号密码
 * @param {String} platformCode 平台代码
 */
export function deletePlatformConfigApi(platformCode) {
  return request({
    url: `/api/platform/config/${platformCode}`,
    method: 'delete'
  })
}
