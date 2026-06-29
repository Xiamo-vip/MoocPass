package top.xiamoi.moocpass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import top.xiamoi.moocpass.entity.QuestionCache;
import top.xiamoi.moocpass.mapper.QuestionCacheMapper;
import top.xiamoi.moocpass.service.QuestionCacheService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 题库客观题缓存服务实现
 */
@Slf4j
@Service
public class QuestionCacheServiceImpl extends ServiceImpl<QuestionCacheMapper, QuestionCache> implements QuestionCacheService {

    // 允许缓存的客观题类型集合（选择题、填空题、判断题）
    private static final Set<String> OBJECTIVE_TYPES = Set.of("single", "multiple", "completion", "judgement", "0", "1", "2", "3");

    @Override
    public String findCachedAnswer(String question, String questionType) {
        if (!isObjectiveQuestion(questionType) || !StringUtils.hasText(question)) {
            return null;
        }

        String hash = computeHash(question);
        try {
            QuestionCache cache = this.getOne(new LambdaQueryWrapper<QuestionCache>()
                    .eq(QuestionCache::getQuestionHash, hash), false);
            if (cache != null && StringUtils.hasText(cache.getAnswer())) {
                log.info("🎯 [题库缓存命中] [{}] 题目: {} → 缓存答案: {}", questionType, truncate(question, 20), cache.getAnswer());
                return cache.getAnswer();
            }
        } catch (Exception e) {
            log.warn("查询题库缓存异常: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public void saveAnswerCache(String question, String questionType, String answer) {
        // 大题主观题坚决不缓存
        if (!isObjectiveQuestion(questionType) || !StringUtils.hasText(question) || !StringUtils.hasText(answer)) {
            return;
        }

        String hash = computeHash(question);
        try {
            QuestionCache exist = this.getOne(new LambdaQueryWrapper<QuestionCache>()
                    .eq(QuestionCache::getQuestionHash, hash), false);
            if (exist == null) {
                QuestionCache newCache = QuestionCache.builder()
                        .questionHash(hash)
                        .question(question)
                        .questionType(questionType != null ? questionType : "objective")
                        .answer(answer)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build();
                this.save(newCache);
                log.debug("💾 [题库缓存写入] [{}] 题目: {}", questionType, truncate(question, 20));
            } else if (!answer.equals(exist.getAnswer())) {
                exist.setAnswer(answer);
                exist.setUpdateTime(LocalDateTime.now());
                this.updateById(exist);
            }
        } catch (Exception e) {
            log.warn("写入题库缓存异常: {}", e.getMessage());
        }
    }

    private boolean isObjectiveQuestion(String type) {
        if (type == null) return true;
        String t = type.toLowerCase().trim();
        // 主观题、大题显式排除
        if (t.contains("subjective") || t.contains("4") || t.contains("主观") || t.contains("简答") || t.contains("论述")) {
            return false;
        }
        return true;
    }

    private String computeHash(String question) {
        // 清理空白字符与干扰符号后计算 MD5
        String clean = question.replaceAll("[\\s\\p{Punct}\\p{Punct}]+", "").toLowerCase();
        return DigestUtils.md5DigestAsHex(clean.getBytes(StandardCharsets.UTF_8));
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
