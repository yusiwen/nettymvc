package cn.yusiwen.nettymvc.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异步消息批量处理，该注解的用户代码将运行在独立的线程组
 *
 * @author yusiwen
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncBatch {

    /**
     * 线程数量
     *
     * @return 线程数量
     */
    int poolSize() default 2;

    /**
     * 最大累计消息数
     *
     * @return 最大累计消息数
     */
    int maxElements() default 4000;

    /**
     * 最大等待时间
     *
     * @return 最大等待时间
     */
    int maxWait() default 1000;

}
