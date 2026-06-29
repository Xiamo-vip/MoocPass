package top.xiamoi.moocpass.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 平台信息展示 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformVO {
    private String code;
    private String name;
    private Boolean bound;
}
