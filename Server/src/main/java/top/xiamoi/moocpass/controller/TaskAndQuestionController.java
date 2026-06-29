package top.xiamoi.moocpass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.xiamoi.moocpass.annotation.RequireLogin;
import top.xiamoi.moocpass.common.Result;
import top.xiamoi.moocpass.common.UserContext;
import top.xiamoi.moocpass.dto.QuestionConfigDTO;
import top.xiamoi.moocpass.dto.StartTaskDTO;
import top.xiamoi.moocpass.entity.CourseTask;
import top.xiamoi.moocpass.entity.UserQuestionConfig;
import top.xiamoi.moocpass.service.CourseTaskService;
import top.xiamoi.moocpass.service.QuestionConfigService;

import java.util.List;

/**
 * 题库设置与刷课任务控制器
 */
@RequireLogin
@RestController
@RequestMapping("/api")
@CrossOrigin
@RequiredArgsConstructor
public class TaskAndQuestionController {

    private final QuestionConfigService questionConfigService;
    private final CourseTaskService courseTaskService;

    /**
     * 获取当前用户的题库/AI配置
     */
    @GetMapping("/question/config")
    public Result<UserQuestionConfig> getQuestionConfig() {
        Long userId = UserContext.getUserId();
        UserQuestionConfig config = questionConfigService.getConfigByUserId(userId);
        return Result.success(config);
    }

    /**
     * 保存/配置 AI 题库 Base URL, Key 和相关设置
     */
    @PostMapping("/question/config")
    public Result<String> saveQuestionConfig(@RequestBody QuestionConfigDTO configDTO) {
        Long userId = UserContext.getUserId();
        questionConfigService.saveOrUpdateConfig(userId, configDTO);
        return Result.success(null, "题库与AI配置保存成功");
    }

    /**
     * 启动刷课任务
     */
    @PostMapping("/task/start")
    public Result<CourseTask> startTask(@RequestBody StartTaskDTO startTaskDTO) {
        Long userId = UserContext.getUserId();
        CourseTask task = courseTaskService.startTask(userId, startTaskDTO);
        return Result.success(task, "刷课任务已成功启动");
    }

    /**
     * 获取任务当前状态与进度
     */
    @GetMapping("/task/{taskId}/status")
    public Result<CourseTask> getTaskStatus(@PathVariable Long taskId) {
        CourseTask task = courseTaskService.getById(taskId);
        return Result.success(task);
    }

    /**
     * 获取任务实时刷课日志列表
     */
    @GetMapping("/task/{taskId}/logs")
    public Result<List<String>> getTaskLogs(@PathVariable Long taskId) {
        List<String> logs = courseTaskService.getTaskLogs(taskId);
        return Result.success(logs);
    }

    /**
     * 获取当前用户的刷课任务列表
     */
    @GetMapping("/task/list")
    public Result<List<CourseTask>> getTaskList() {
        Long userId = UserContext.getUserId();
        List<CourseTask> tasks = courseTaskService.getTaskList(userId);
        return Result.success(tasks);
    }

    /**
     * 停止正在运行的手动刷课任务
     */
    @PostMapping("/task/{taskId}/stop")
    public Result<String> stopTask(@PathVariable Long taskId) {
        Long userId = UserContext.getUserId();
        courseTaskService.stopTask(userId, taskId);
        return Result.success(null, "刷课任务已暂停/停止");
    }

    /**
     * 删除任务记录
     */
    @DeleteMapping("/task/{taskId}")
    public Result<String> deleteTask(@PathVariable Long taskId) {
        Long userId = UserContext.getUserId();
        courseTaskService.deleteTask(userId, taskId);
        return Result.success(null, "任务记录已删除");
    }
}
