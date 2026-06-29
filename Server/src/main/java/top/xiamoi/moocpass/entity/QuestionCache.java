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
 * 题库客观题缓存实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("question_cache")
public class QuestionCache implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题目原文本 MD5 哈希
     */
    private String questionHash;

    /**
     * 题目原文本
     */
    private String question;

    /**
     * 题目类型：single, multiple, completion, judgement
     */
    private String questionType;

    /**
     * 答案内容
     */
    private String answer;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
