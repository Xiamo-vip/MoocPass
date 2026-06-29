package top.xiamoi.moocpass.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.xiamoi.moocpass.entity.QuestionCache;

/**
 * 题库客观题缓存服务接口
 */
public interface QuestionCacheService extends IService<QuestionCache> {

    /**
     * 根据题目与类型查找缓存的答案（仅支持客观题）
     */
    String findCachedAnswer(String question, String questionType);

    /**
     * 保存客观题答案到缓存
     */
    void saveAnswerCache(String question, String questionType, String answer);
}
