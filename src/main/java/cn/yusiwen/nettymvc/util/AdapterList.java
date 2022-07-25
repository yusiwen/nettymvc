package cn.yusiwen.nettymvc.util;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;

/**
 * @author yusiwen
 * @param <S>
 *            Source
 * @param <T>
 *            Target
 */
public final class AdapterList<S, T> extends AbstractList<T> {

    /**
     * Source list
     */
    private final List<S> src;
    /**
     * Functions
     */
    private final Function<S, T> function;

    public AdapterList(List<S> src, Function<S, T> function) {
        this.src = src;
        this.function = function;
    }

    @Override
    public T get(int index) {
        return function.apply(src.get(index));
    }

    @Override
    public int size() {
        return src.size();
    }
}