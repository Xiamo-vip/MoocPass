package top.xiamoi.moocpass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import top.xiamoi.moocpass.dto.PlatformConfigDTO;
import top.xiamoi.moocpass.entity.UserPlatformConfig;
import top.xiamoi.moocpass.mapper.UserPlatformConfigMapper;
import top.xiamoi.moocpass.platform.MoocPlatformAdapter;
import top.xiamoi.moocpass.platform.PlatformAdapterRegistry;
import top.xiamoi.moocpass.service.PlatformConfigService;

import java.time.LocalDateTime;

/**
 * 用户网课平台账号设置服务实现
 */
@Service
@RequiredArgsConstructor
public class PlatformConfigServiceImpl extends ServiceImpl<UserPlatformConfigMapper, UserPlatformConfig> implements PlatformConfigService {

    private final PlatformAdapterRegistry adapterRegistry;

    @Override
    public UserPlatformConfig getConfig(Long userId, String platformCode) {
        return this.getOne(new LambdaQueryWrapper<UserPlatformConfig>()
                .eq(UserPlatformConfig::getUserId, userId)
                .eq(UserPlatformConfig::getPlatformCode, platformCode));
    }

    @Override
    public boolean saveOrUpdateConfig(Long userId, PlatformConfigDTO configDTO) {
        if (configDTO == null || !StringUtils.hasText(configDTO.getPlatformCode())) {
            throw new RuntimeException("平台代码不能为空");
        }

        MoocPlatformAdapter adapter = adapterRegistry.getAdapter(configDTO.getPlatformCode());
        if (adapter == null) {
            throw new RuntimeException("暂不支持该网课平台：" + configDTO.getPlatformCode());
        }

        String username = configDTO.getUsername();
        String password = configDTO.getPassword();
        String token = configDTO.getToken();

        // 验证账号或 Token 有效性
        if (StringUtils.hasText(token)) {
            boolean valid = adapter.validateAuth(token);
            if (!valid) {
                throw new RuntimeException("账号绑定失败：OAuth Token 无效或已失效");
            }
            if (!StringUtils.hasText(username)) {
                username = "职教云用户";
            }
        } else {
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                throw new RuntimeException("平台账号和密码不能为空");
            }
            boolean valid = adapter.validateAccount(username, password);
            if (!valid) {
                throw new RuntimeException("账号绑定失败：平台账号或密码错误，无法完成登录验证");
            }
        }

        UserPlatformConfig existConfig = getConfig(userId, configDTO.getPlatformCode());
        if (existConfig == null) {
            existConfig = UserPlatformConfig.builder()
                    .userId(userId)
                    .platformCode(configDTO.getPlatformCode())
                    .username(username)
                    .password(password)
                    .token(token)
                    .status(1)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
        } else {
            existConfig.setUsername(username);
            existConfig.setPassword(password);
            existConfig.setToken(token);
            existConfig.setStatus(1);
            existConfig.setUpdateTime(LocalDateTime.now());
        }

        return this.saveOrUpdate(existConfig);
    }


    @Override
    public boolean deleteConfig(Long userId, String platformCode) {
        return this.remove(new LambdaQueryWrapper<UserPlatformConfig>()
                .eq(UserPlatformConfig::getUserId, userId)
                .eq(UserPlatformConfig::getPlatformCode, platformCode));
    }
}
