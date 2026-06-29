package top.xiamoi.moocpass.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.xiamoi.moocpass.dto.PlatformConfigDTO;
import top.xiamoi.moocpass.entity.UserPlatformConfig;

/**
 * 用户网课平台账号设置服务接口
 */
public interface PlatformConfigService extends IService<UserPlatformConfig> {

    /**
     * 获取指定用户的网课平台配置
     */
    UserPlatformConfig getConfig(Long userId, String platformCode);

    /**
     * 保存或更新用户的网课平台账号设置（包含账号密码有效性校验）
     */
    boolean saveOrUpdateConfig(Long userId, PlatformConfigDTO configDTO);

    /**
     * 解绑/删除用户的网课平台账号设置
     */
    boolean deleteConfig(Long userId, String platformCode);
}
