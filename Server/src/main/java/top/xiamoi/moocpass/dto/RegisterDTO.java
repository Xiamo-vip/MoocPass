package top.xiamoi.moocpass.dto;

import lombok.Data;

/**
 * 用户注册请求 DTO
 */
@Data
public class RegisterDTO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 密码
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;
}
