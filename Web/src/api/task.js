import request from '../utils/request'

/**
 * 获取当前用户的题库/AI配置
 */
export function getQuestionConfigApi() {
  return request({
    url: '/api/question/config',
    method: 'get'
  })
}

/**
 * 保存/配置 AI 题库 Base URL, Key 和相关设置
 * @param {Object} data { provider, aiBaseUrl, aiKey, aiModel, submit, coverRate, minIntervalSeconds }
 */
export function saveQuestionConfigApi(data) {
  return request({
    url: '/api/question/config',
    method: 'post',
    data
  })
}

/**
 * 启动刷课任务
 * @param {Object} data { platformCode, courseId, classId, courseName, coverUrl, teacher, speed }
 */
export function startTaskApi(data) {
  return request({
    url: '/api/task/start',
    method: 'post',
    data
  })
}

/**
 * 获取任务当前状态与进度
 * @param {Number|String} taskId
 */
export function getTaskStatusApi(taskId) {
  return request({
    url: `/api/task/${taskId}/status`,
    method: 'get'
  })
}

/**
 * 获取任务实时刷课日志列表
 * @param {Number|String} taskId
 */
export function getTaskLogsApi(taskId) {
  return request({
    url: `/api/task/${taskId}/logs`,
    method: 'get'
  })
}

/**
 * 获取用户的任务列表
 */
export function getTaskListApi() {
  return request({
    url: '/api/task/list',
    method: 'get'
  })
}

/**
 * 停止正在运行的任务
 * @param {Number|String} taskId
 */
export function stopTaskApi(taskId) {
  return request({
    url: `/api/task/${taskId}/stop`,
    method: 'post'
  })
}

/**
 * 删除任务记录
 * @param {Number|String} taskId
 */
export function deleteTaskApi(taskId) {
  return request({
    url: `/api/task/${taskId}`,
    method: 'delete'
  })
}
