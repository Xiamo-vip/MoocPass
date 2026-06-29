package top.xiamoi.moocpass.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课程信息展示 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseVO {
    private String courseId;
    private String classId;
    private String name;
    private String teacher;
    private String coverUrl;
}
