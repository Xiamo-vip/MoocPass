package top.xiamoi.moocpass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import top.xiamoi.moocpass.dto.StartTaskDTO;
import top.xiamoi.moocpass.entity.CourseTask;
import top.xiamoi.moocpass.entity.UserPlatformConfig;
import top.xiamoi.moocpass.entity.UserQuestionConfig;
import top.xiamoi.moocpass.mapper.CourseTaskMapper;
import top.xiamoi.moocpass.platform.MoocPlatformAdapter;
import top.xiamoi.moocpass.platform.PlatformAdapterRegistry;
import top.xiamoi.moocpass.service.CourseTaskService;
import top.xiamoi.moocpass.service.PlatformConfigService;
import top.xiamoi.moocpass.service.QuestionConfigService;
import top.xiamoi.moocpass.solver.QuestionSolver;
import top.xiamoi.moocpass.solver.QuestionSolverFactory;
import top.xiamoi.moocpass.task.TaskLogger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 刷课任务服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseTaskServiceImpl extends ServiceImpl<CourseTaskMapper, CourseTask> implements CourseTaskService, CommandLineRunner {

    private final PlatformConfigService platformConfigService;
    private final QuestionConfigService questionConfigService;
    private final PlatformAdapterRegistry adapterRegistry;
    private final QuestionSolverFactory solverFactory;
    private final TaskLogger taskLogger;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<Long, Future<?>> activeTaskFutures = new ConcurrentHashMap<>();

    /**
     * 系统启动自动唤醒云端未完成任务，实现服务崩溃/重启恢复
     */
    @Override
    public void run(String... args) {
        log.info("正在检查数据库未完成的任务...");
        try {
            List<CourseTask> runningTasks = this.list(new LambdaQueryWrapper<CourseTask>()
                    .eq(CourseTask::getStatus, "RUNNING"));
            if (runningTasks != null && !runningTasks.isEmpty()) {
                log.info("检测到 {} 个重启前未完成的挂机任务，正在无缝恢复挂机线程...", runningTasks.size());
                for (CourseTask task : runningTasks) {
                    taskLogger.log(task.getId(), "后端服务重启/崩溃恢复机制已触发，正在自动接管并恢复挂机...");
                    submitTaskExecution(task);
                }
            } else {
                log.info("云端引擎初始化就绪。");
            }
        } catch (Exception e) {
            log.error("系统启动自动唤醒任务失败：", e);
        }
    }

    @Override
    public CourseTask startTask(Long userId, StartTaskDTO startTaskDTO) {
        if (startTaskDTO == null || !StringUtils.hasText(startTaskDTO.getPlatformCode()) || !StringUtils.hasText(startTaskDTO.getCourseId())) {
            throw new RuntimeException("平台代码和课程ID不能为空");
        }

        MoocPlatformAdapter adapter = adapterRegistry.getAdapter(startTaskDTO.getPlatformCode());
        if (adapter == null) {
            throw new RuntimeException("不支持的网课平台：" + startTaskDTO.getPlatformCode());
        }

        UserPlatformConfig platformConfig = platformConfigService.getConfig(userId, startTaskDTO.getPlatformCode());
        if (platformConfig == null || platformConfig.getStatus() == 0) {
            throw new RuntimeException("请先绑定该平台的账号密码");
        }

        UserQuestionConfig questionConfig = questionConfigService.getConfigByUserId(userId);
        String selectedProvider = StringUtils.hasText(startTaskDTO.getQuestionProvider())
                ? startTaskDTO.getQuestionProvider()
                : (questionConfig != null && StringUtils.hasText(questionConfig.getProvider()) ? questionConfig.getProvider() : "AI");

        if ("AI".equalsIgnoreCase(selectedProvider)) {
            if (questionConfig == null || !StringUtils.hasText(questionConfig.getAiBaseUrl()) || !StringUtils.hasText(questionConfig.getAiKey())) {
                throw new RuntimeException("未检测到有效的 AI 题库配置，请先配置 AI BaseUrl 和 API Key");
            }
        }

        CourseTask task = CourseTask.builder()
                .userId(userId)
                .platformCode(startTaskDTO.getPlatformCode())
                .courseId(startTaskDTO.getCourseId())
                .classId(startTaskDTO.getClassId())
                .courseName(startTaskDTO.getCourseName())
                .coverUrl(startTaskDTO.getCoverUrl())
                .teacher(startTaskDTO.getTeacher())
                .speed(startTaskDTO.getSpeed() != null ? startTaskDTO.getSpeed() : 1.0)
                .questionProvider(selectedProvider)
                .autoAnswer(startTaskDTO.getAutoAnswer() != null ? startTaskDTO.getAutoAnswer() : true)
                .submitAnswer(startTaskDTO.getSubmitAnswer() != null ? startTaskDTO.getSubmitAnswer() : true)
                .coverRate(startTaskDTO.getCoverRate() != null ? startTaskDTO.getCoverRate() : 0.8)
                .status("RUNNING")
                .progress(0)
                .currentChapter("准备开始")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        this.save(task);

        submitTaskExecution(task);

        return task;
    }

    /**
     * 内部通用线程提交与句柄管理方法
     */
    private void submitTaskExecution(CourseTask task) {
        Long taskId = task.getId();

        Future<?> oldFuture = activeTaskFutures.remove(taskId);
        if (oldFuture != null && !oldFuture.isDone()) {
            oldFuture.cancel(true);
        }

        Future<?> future = executorService.submit(() -> {
            try {
                MoocPlatformAdapter adapter = adapterRegistry.getAdapter(task.getPlatformCode());
                UserPlatformConfig platformConfig = platformConfigService.getConfig(task.getUserId(), task.getPlatformCode());
                UserQuestionConfig questionConfig = questionConfigService.getConfigByUserId(task.getUserId());

                String provider = StringUtils.hasText(task.getQuestionProvider()) ? task.getQuestionProvider()
                        : (questionConfig != null && StringUtils.hasText(questionConfig.getProvider()) ? questionConfig.getProvider() : "AI");
                QuestionSolver questionSolver = solverFactory.getSolver(provider);

                if (adapter != null && platformConfig != null) {
                    adapter.executeCourseTask(task, platformConfig, questionConfig, questionSolver, taskLogger);
                    CourseTask current = this.getById(taskId);
                    if (current != null && "RUNNING".equalsIgnoreCase(current.getStatus())) {
                        current.setStatus("COMPLETED");
                        current.setProgress(100);
                        current.setUpdateTime(LocalDateTime.now());
                        this.updateById(current);
                        taskLogger.log(taskId, "刷课任务已全部完成！");
                    }
                }
            } catch (Exception e) {
                log.error("刷课任务执行异常 [taskId={}]:", taskId, e);
                CourseTask current = this.getById(taskId);
                if (current != null && !"STOPPED".equalsIgnoreCase(current.getStatus())) {
                    current.setStatus("FAILED");
                    current.setErrorMessage(e.getMessage());
                    current.setUpdateTime(LocalDateTime.now());
                    this.updateById(current);
                }
            } finally {
                activeTaskFutures.remove(taskId);
            }
        });

        activeTaskFutures.put(taskId, future);
    }

    @Override
    public List<String> getTaskLogs(Long taskId) {
        return taskLogger.getLogs(taskId);
    }

    @Override
    public List<CourseTask> getTaskList(Long userId) {
        return this.list(new LambdaQueryWrapper<CourseTask>()
                .eq(CourseTask::getUserId, userId)
                .orderByDesc(CourseTask::getCreateTime));
    }

    @Override
    public boolean stopTask(Long userId, Long taskId) {
        CourseTask task = this.getOne(new LambdaQueryWrapper<CourseTask>()
                .eq(CourseTask::getId, taskId)
                .eq(CourseTask::getUserId, userId));
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        task.setStatus("STOPPED");
        task.setUpdateTime(LocalDateTime.now());
        taskLogger.log(taskId, "用户手动停止了云端挂机任务");
        boolean updated = this.updateById(task);
        Future<?> future = activeTaskFutures.remove(taskId);
        if (future != null) {
            future.cancel(true);
        }
        return updated;
    }

    @Override
    public boolean deleteTask(Long userId, Long taskId) {
        Future<?> future = activeTaskFutures.remove(taskId);
        if (future != null) {
            future.cancel(true);
        }
        taskLogger.clearLogs(taskId);
        return this.remove(new LambdaQueryWrapper<CourseTask>()
                .eq(CourseTask::getId, taskId)
                .eq(CourseTask::getUserId, userId));
    }
}
