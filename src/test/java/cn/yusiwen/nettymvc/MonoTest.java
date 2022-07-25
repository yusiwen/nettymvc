package cn.yusiwen.nettymvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.yusiwen.nettymvc.session.Session;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

/**
 * @author yusiwen
 */
public class MonoTest {

    private static final Logger log = LoggerFactory.getLogger(Session.class.getSimpleName());

    public static void main(String[] args) {
        Hooks.onErrorDropped(t -> log.warn("Reactor", t));

        Mono<Integer> mono = Mono.<Double> create(sink -> {
            log.info("connect a");
            new Thread(() -> {
                log.info("receive a");
                try {
                    double value = (1 / 1);
                    sink.success(value);
                } catch (Throwable e) {
                    sink.error(new Exception("calc error"));
                }
            }).start();
            sink.onDispose(() -> log.info("release a"));
        }).then(Mono.create(sink -> {
            log.info("connect b");
            new Thread(() -> {
                log.info("receive b");
                sink.success(1 + 1);
            }).start();
            sink.onDispose(() -> log.info("release b"));
        }));

        mono.doOnSuccess(value -> log.info("doOnSuccess1: " + value))
                .doOnSuccess(value -> log.info("doOnSuccess2: " + value)).doOnError(throwable -> log.info("doOnError"))
                .doFinally(signalType -> log.info("doFinally")).subscribe();
    }
}
