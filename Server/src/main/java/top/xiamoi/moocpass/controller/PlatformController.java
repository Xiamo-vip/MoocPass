package top.xiamoi.moocpass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.xiamoi.moocpass.annotation.RequireLogin;
import top.xiamoi.moocpass.common.Result;
import top.xiamoi.moocpass.common.UserContext;
import top.xiamoi.moocpass.dto.PlatformConfigDTO;
import top.xiamoi.moocpass.entity.UserPlatformConfig;
import top.xiamoi.moocpass.platform.MoocPlatformAdapter;
import top.xiamoi.moocpass.platform.PlatformAdapterRegistry;
import top.xiamoi.moocpass.service.PlatformConfigService;
import top.xiamoi.moocpass.vo.CourseVO;
import top.xiamoi.moocpass.vo.PlatformVO;

import java.util.ArrayList;
import java.util.List;

@RequireLogin
@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
@CrossOrigin
public class PlatformController {

    private final PlatformAdapterRegistry adapterRegistry;
    private final PlatformConfigService platformConfigService;

    /**
     * 获取支持的刷课平台列表及绑定状态
     */
    @GetMapping("/list")
    public Result<List<PlatformVO>> getPlatformList() {
        Long userId = UserContext.getUserId();
        List<MoocPlatformAdapter> adapters = adapterRegistry.getAllAdapters();
        List<PlatformVO> result = new ArrayList<>();

        for (MoocPlatformAdapter adapter : adapters) {
            UserPlatformConfig config = platformConfigService.getConfig(userId, adapter.getPlatformCode());
            boolean bound = config != null && config.getStatus() == 1;

            result.add(PlatformVO.builder()
                    .code(adapter.getPlatformCode())
                    .name(adapter.getPlatformName())
                    .bound(bound)
                    .build());
        }
        return Result.success(result);
    }

    /**
     * 获取用户在特定平台的绑定账号设置
     */
    @GetMapping("/config/{platformCode}")
    public Result<UserPlatformConfig> getPlatformConfig(@PathVariable String platformCode) {
        Long userId = UserContext.getUserId();
        UserPlatformConfig config = platformConfigService.getConfig(userId, platformCode);
        return Result.success(config);
    }

    /**
     * 绑定或更新网课平台账号密码
     */
    @PostMapping("/config")
    public Result<String> savePlatformConfig(@RequestBody PlatformConfigDTO configDTO) {
        Long userId = UserContext.getUserId();
        platformConfigService.saveOrUpdateConfig(userId, configDTO);
        return Result.success(null, "账号绑定设置成功");
    }

    /**
     * 获取用户在指定平台的课程列表 (显示封面、老师、课程名、课程ID)
     */
    @GetMapping("/courses/{platformCode}")
    public Result<List<CourseVO>> getCourseList(@PathVariable String platformCode) {
        Long userId = UserContext.getUserId();
        UserPlatformConfig config = platformConfigService.getConfig(userId, platformCode);

        if (config == null || config.getStatus() == 0) {
            return Result.error(400, "您尚未绑定该平台账号，请先完成账号绑定");
        }

        MoocPlatformAdapter adapter = adapterRegistry.getAdapter(platformCode);
        if (adapter == null) {
            return Result.error(400, "暂不支持该平台：" + platformCode);
        }

        List<CourseVO> courses = adapter.getCourseList(config);
        return Result.success(courses);

    }

    /**
     * 解绑/删除特定平台的账号配置
     */
    @DeleteMapping("/config/{platformCode}")
    public Result<String> deletePlatformConfig(@PathVariable String platformCode) {
        Long userId = UserContext.getUserId();
        platformConfigService.deleteConfig(userId, platformCode);
        return Result.success(null, "平台账号解绑成功");
    }
}
