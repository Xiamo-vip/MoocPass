package top.xiamoi.moocpass.solver;

import top.xiamoi.moocpass.entity.UserQuestionConfig;

import java.util.List;

/**
 * 题库搜题解题策略接口
 */
public interface QuestionSolver {

    /**
     * 获取题库提供者代码（如 AI, TikuGo）
     */
    String getProviderCode();

    /**
     * 搜题与答题
     *
     * @param question 题目内容
     * @param options  选项列表
     * @param config   用户的题库配置
     * @return 解答答案/选项
     */
    String solve(String question, List<String> options, UserQuestionConfig config);
}
