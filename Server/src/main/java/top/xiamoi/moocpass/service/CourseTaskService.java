package top.xiamoi.moocpass.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.xiamoi.moocpass.dto.StartTaskDTO;
import top.xiamoi.moocpass.entity.CourseTask;

import java.util.List;

/**
 * 刷课任务服务接口
 */
public interface CourseTaskService extends IService<CourseTask> {

    /**
     * 启动刷课任务
     */
    CourseTask startTask(Long userId, StartTaskDTO startTaskDTO);

    /**
     * 获取指定任务的实时日志
     */
    List<String> getTaskLogs(Long taskId);

    /**
     * 获取指定用户的任务列表
     */
    List<CourseTask> getTaskList(Long userId);

    /**
     * 停止正在运行的任务
     */
    boolean stopTask(Long userId, Long taskId);

    /**
     * 删除任务记录
     */
    boolean deleteTask(Long userId, Long taskId);
}
