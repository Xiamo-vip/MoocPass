package top.xiamoi.moocpass.dto;

import lombok.Data;

/**
 * 用户登录请求 DTO
 */
@Data
public class LoginDTO {
    /**
     * 用户名或邮箱
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
