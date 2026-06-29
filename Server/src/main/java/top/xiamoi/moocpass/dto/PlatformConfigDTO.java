package top.xiamoi.moocpass.dto;

import lombok.Data;

/**
 * 网课平台绑定/配置请求 DTO
 */
@Data
public class PlatformConfigDTO {
    private String platformCode;
    private String username;
    private String password;
    private String token;
}

