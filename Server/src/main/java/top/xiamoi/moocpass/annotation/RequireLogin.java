package top.xiamoi.moocpass.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireLogin {
    /**
     * 是否必须登录，默认为 true
     */
    boolean required() default true;
}
