package top.xiamoi.moocpass.platform;

import top.xiamoi.moocpass.entity.CourseTask;
import top.xiamoi.moocpass.entity.UserPlatformConfig;
import top.xiamoi.moocpass.entity.UserQuestionConfig;
import top.xiamoi.moocpass.solver.QuestionSolver;
import top.xiamoi.moocpass.task.TaskLogger;
import top.xiamoi.moocpass.vo.CourseVO;

import java.util.List;

/**
 * 多网课平台统一适配器接口
 */
public interface MoocPlatformAdapter {

    /**
     * 获取平台唯一代码（如 chaoxing, zhihuishu）
     */
    String getPlatformCode();

    /**
     * 获取平台中文名称（如 超星学习通）
     */
    String getPlatformName();

    /**
     * 校验账号密码是否能成功登录
     */
    boolean validateAccount(String username, String password);

    boolean validateAuth(String token);

    /**
     * 获取用户在当前平台的课程列表
     */
    List<CourseVO> getCourseList(UserPlatformConfig platformConfig);


    /**
     * 执行指定课程的刷课任务
     */
    void executeCourseTask(CourseTask task,
                           UserPlatformConfig platformConfig,
                           UserQuestionConfig questionConfig,
                           QuestionSolver questionSolver,
                           TaskLogger taskLogger);
}
