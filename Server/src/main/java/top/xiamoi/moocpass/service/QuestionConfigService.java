package top.xiamoi.moocpass.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.xiamoi.moocpass.dto.QuestionConfigDTO;
import top.xiamoi.moocpass.entity.UserQuestionConfig;

/**
 * 用户题库与AI配置服务接口
 */
public interface QuestionConfigService extends IService<UserQuestionConfig> {

    /**
     * 获取指定用户的题库/AI配置
     */
    UserQuestionConfig getConfigByUserId(Long userId);

    /**
     * 保存或更新用户的题库与AI配置
     */
    boolean saveOrUpdateConfig(Long userId, QuestionConfigDTO configDTO);
}
