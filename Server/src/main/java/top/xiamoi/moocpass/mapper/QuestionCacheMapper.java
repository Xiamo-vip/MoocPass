package top.xiamoi.moocpass.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.xiamoi.moocpass.entity.QuestionCache;

/**
 * 题库缓存 Mapper 接口
 */
@Mapper
public interface QuestionCacheMapper extends BaseMapper<QuestionCache> {
}
