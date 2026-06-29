package top.xiamoi.moocpass.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 刷课任务持久化全量日志实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("course_task_log")
public class CourseTaskLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 刷课任务ID (UNIQUE)
     */
    private Long taskId;

    /**
     * 全量运行日志文本 (换行符分隔)
     */
    private String fullLog;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
