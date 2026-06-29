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
 * 用户题库/AI答题配置实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_question_config")
public class UserQuestionConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属系统用户ID
     */
    private Long userId;

    /**
     * 题库类型：AI, TikuGo, TikuYanxi 等
     */
    private String provider;

    /**
     * AI 接口 Base URL
     */
    private String aiBaseUrl;

    /**
     * AI API Key
     */
    private String aiKey;

    /**
     * AI 模型名称（如 deepseek-chat）
     */
    private String aiModel;

    /**
     * 是否提交答题
     */
    private Boolean submit;

    /**
     * 最低题库覆盖率（0.0 - 1.0）
     */
    private Double coverRate;

    /**
     * 各题库/解题器独立配置 JSON (如 AI, TikuGo, TikuYanxi 等各自配置参数)
     */
    private String configJson;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
