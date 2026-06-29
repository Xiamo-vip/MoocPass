package top.xiamoi.moocpass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import top.xiamoi.moocpass.dto.QuestionConfigDTO;
import top.xiamoi.moocpass.entity.UserQuestionConfig;
import top.xiamoi.moocpass.mapper.UserQuestionConfigMapper;
import top.xiamoi.moocpass.service.QuestionConfigService;

import java.time.LocalDateTime;

/**
 * 用户题库与AI配置服务实现
 */
@Service
public class QuestionConfigServiceImpl extends ServiceImpl<UserQuestionConfigMapper, UserQuestionConfig> implements QuestionConfigService {

    @Override
    public UserQuestionConfig getConfigByUserId(Long userId) {
        return this.getOne(new LambdaQueryWrapper<UserQuestionConfig>()
                .eq(UserQuestionConfig::getUserId, userId));
    }

    @Override
    public boolean saveOrUpdateConfig(Long userId, QuestionConfigDTO configDTO) {
        if (configDTO == null) {
            throw new RuntimeException("配置参数不能为空");
        }

        UserQuestionConfig existConfig = getConfigByUserId(userId);
        if (existConfig == null) {
            existConfig = UserQuestionConfig.builder()
                    .userId(userId)
                    .provider(StringUtils.hasText(configDTO.getProvider()) ? configDTO.getProvider() : "AI")
                    .aiBaseUrl(configDTO.getAiBaseUrl())
                    .aiKey(configDTO.getAiKey())
                    .aiModel(configDTO.getAiModel())
                    .submit(configDTO.getSubmit() != null ? configDTO.getSubmit() : true)
                    .coverRate(configDTO.getCoverRate() != null ? configDTO.getCoverRate() : 0.9)
                    .configJson(configDTO.getConfigJson())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
        } else {
            if (StringUtils.hasText(configDTO.getProvider())) existConfig.setProvider(configDTO.getProvider());
            if (configDTO.getAiBaseUrl() != null) existConfig.setAiBaseUrl(configDTO.getAiBaseUrl());
            if (configDTO.getAiKey() != null) existConfig.setAiKey(configDTO.getAiKey());
            if (configDTO.getAiModel() != null) existConfig.setAiModel(configDTO.getAiModel());
            if (configDTO.getSubmit() != null) existConfig.setSubmit(configDTO.getSubmit());
            if (configDTO.getCoverRate() != null) existConfig.setCoverRate(configDTO.getCoverRate());
            if (configDTO.getConfigJson() != null) existConfig.setConfigJson(configDTO.getConfigJson());
            existConfig.setUpdateTime(LocalDateTime.now());
        }

        return this.saveOrUpdate(existConfig);
    }
}
