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
 * 刷课任务实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("course_task")
public class CourseTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属系统用户ID
     */
    private Long userId;

    /**
     * 平台代码
     */
    private String platformCode;

    /**
     * 网课平台课程ID
     */
    private String courseId;

    /**
     * 网课平台班级ID（特定平台如超星需要）
     */
    private String classId;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程封面图片URL
     */
    private String coverUrl;

    /**
     * 任课老师
     */
    private String teacher;

    /**
     * 播放倍速
     */
    private Double speed;

    /**
     * 任务状态：PENDING, RUNNING, COMPLETED, FAILED, STOPPED
     */
    private String status;

    /**
     * 完成进度 percentage (0 - 100)
     */
    private Integer progress;

    /**
     * 当前处理章节
     */
    private String currentChapter;

    /**
     * 所选题库策略 (AI 等)
     */
    private String questionProvider;

    /**
     * 是否开启自动答题
     */
    private Boolean autoAnswer;

    /**
     * 答题处理模式：true 达到覆盖率提交，false 仅保存
     */
    private Boolean submitAnswer;

    /**
     * 最小答题覆盖率 (0.0 - 1.0)
     */
    private Double coverRate;

    /**
     * 错误说明信息
     */
    private String errorMessage;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
