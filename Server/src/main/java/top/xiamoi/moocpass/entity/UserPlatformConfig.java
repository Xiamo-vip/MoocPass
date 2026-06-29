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
 * 用户网课平台配置实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_platform_config")
public class UserPlatformConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属系统用户ID
     */
    private Long userId;

    /**
     * 平台代码（如 chaoxing）
     */
    private String platformCode;

    /**
     * 平台账号/手机号
     */
    private String username;

    /**
     * 平台密码
     */
    private String password;
    
    /**
     * 平台 OAuth 认证凭证 Token
     */
    private String token;

    /**
     * 状态：1 正常，0 异常/未绑定
     */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
