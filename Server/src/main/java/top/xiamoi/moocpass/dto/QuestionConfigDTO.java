package top.xiamoi.moocpass.dto;

import lombok.Data;

/**
 * 题库与AI配置请求 DTO
 */
@Data
public class QuestionConfigDTO {
    private String provider;
    private String aiBaseUrl;
    private String aiKey;
    private String aiModel;
    private Boolean submit;
    private Double coverRate;
    private Integer minIntervalSeconds;
    private String configJson;
}
