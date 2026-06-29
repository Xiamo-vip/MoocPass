package top.xiamoi.moocpass.platform;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网课平台适配器注册表（基于策略模式与多态）
 */
@Component
public class PlatformAdapterRegistry {

    private final Map<String, MoocPlatformAdapter> adapterMap = new HashMap<>();

    public PlatformAdapterRegistry(List<MoocPlatformAdapter> adapters) {
        for (MoocPlatformAdapter adapter : adapters) {
            adapterMap.put(adapter.getPlatformCode().toLowerCase(), adapter);
        }
    }

    /**
     * 根据平台代码获取具体的平台适配器
     */
    public MoocPlatformAdapter getAdapter(String platformCode) {
        if (platformCode == null) {
            return null;
        }
        return adapterMap.get(platformCode.toLowerCase());
    }

    /**
     * 获取系统支持的所有平台列表
     */
    public List<MoocPlatformAdapter> getAllAdapters() {
        return new ArrayList<>(adapterMap.values());
    }
}
