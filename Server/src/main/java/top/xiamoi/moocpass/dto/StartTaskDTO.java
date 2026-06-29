package top.xiamoi.moocpass.dto;

import lombok.Data;

/**
 * 启动刷课任务 DTO
 */
@Data
public class StartTaskDTO {
    private String platformCode;
    private String courseId;
    private String classId;
    private String courseName;
    private String coverUrl;
    private String teacher;
    private Double speed;
    private String questionProvider;
    private Boolean autoAnswer;
    private Boolean submitAnswer;
    private Double coverRate;
}
