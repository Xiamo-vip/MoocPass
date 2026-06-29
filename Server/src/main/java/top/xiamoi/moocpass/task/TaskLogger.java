package top.xiamoi.moocpass.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.xiamoi.moocpass.entity.CourseTaskLog;
import top.xiamoi.moocpass.mapper.CourseTaskLogMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刷课任务持久化日志管理器（云端聚合存储，一任务一记录）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskLogger {

    private static final int MAX_LOGS_PER_TASK = 300;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final CourseTaskLogMapper courseTaskLogMapper;

    // 内存的高效日志缓存列表 (taskId -> List<String>)
    private final Map<Long, List<String>> memoryLogs = new ConcurrentHashMap<>();

    /**
     * 记录日志并同步更新云端唯一聚合记录
     */
    public synchronized void log(Long taskId, String message) {
        if (taskId == null || message == null) return;

        // 1. 过滤清理掉所有的树状结构符号，使日志文本干练规整
        String cleanMessage = cleanTreeSymbols(message);
        String timeStr = LocalDateTime.now().format(FORMATTER);
        String formattedLog = String.format("[%s] %s", timeStr, cleanMessage);

        // 2. 更新内存缓存
        List<String> logs = memoryLogs.computeIfAbsent(taskId, k -> loadLogsFromDb(k));
        logs.add(formattedLog);
        if (logs.size() > MAX_LOGS_PER_TASK) {
            logs.remove(0);
        }

        // 3. 聚合成完整大块文本，并持久化到数据库中（一个任务对应一条数据）
        String fullLogText = String.join("\n", logs);
        saveOrUpdateDbLog(taskId, fullLogText);
    }

    /**
     * 获取指定任务的所有日志
     */
    public List<String> getLogs(Long taskId) {
        if (taskId == null) return new ArrayList<>();
        List<String> logs = memoryLogs.get(taskId);
        if (logs == null) {
            logs = loadLogsFromDb(taskId);
            memoryLogs.put(taskId, logs);
        }
        return new ArrayList<>(logs);
    }

    /**
     * 清理任务日志（内存 + 云端数据库）
     */
    public synchronized void clearLogs(Long taskId) {
        if (taskId == null) return;
        memoryLogs.remove(taskId);
        try {
            courseTaskLogMapper.delete(new LambdaQueryWrapper<CourseTaskLog>()
                    .eq(CourseTaskLog::getTaskId, taskId));
        } catch (Exception e) {
            log.error("清理云端数据库任务日志失败: taskId={}", taskId, e);
        }
    }

    /**
     * 从数据库装载任务全量日志
     */
    private List<String> loadLogsFromDb(Long taskId) {
        try {
            CourseTaskLog logEntity = courseTaskLogMapper.selectOne(
                    new LambdaQueryWrapper<CourseTaskLog>().eq(CourseTaskLog::getTaskId, taskId)
            );
            if (logEntity != null && StringUtils.hasText(logEntity.getFullLog())) {
                String[] lines = logEntity.getFullLog().split("\n");
                return new ArrayList<>(Arrays.asList(lines));
            }
        } catch (Exception e) {
            log.error("从云端数据库装载日志失败: taskId={}", taskId, e);
        }
        return new ArrayList<>();
    }

    /**
     * 更新或新增云端持久化表记录
     */
    private void saveOrUpdateDbLog(Long taskId, String fullLogText) {
        try {
            CourseTaskLog existLog = courseTaskLogMapper.selectOne(
                    new LambdaQueryWrapper<CourseTaskLog>().eq(CourseTaskLog::getTaskId, taskId)
            );
            if (existLog == null) {
                CourseTaskLog newLog = CourseTaskLog.builder()
                        .taskId(taskId)
                        .fullLog(fullLogText)
                        .updateTime(LocalDateTime.now())
                        .build();
                courseTaskLogMapper.insert(newLog);
            } else {
                existLog.setFullLog(fullLogText);
                existLog.setUpdateTime(LocalDateTime.now());
                courseTaskLogMapper.updateById(existLog);
            }
        } catch (Exception e) {
            log.error("更新云端持久化日志失败: taskId={}", taskId, e);
        }
    }

    /**
     * 清除日志中的树状图形符号，使排版更加清爽明了
     */
    private String cleanTreeSymbols(String msg) {
        if (msg == null) return "";
        return msg.replaceAll("[│├─└─]+", "").replaceAll("^\\s+", "");
    }
}
