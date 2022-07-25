package cn.yusiwen.nettymvc.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author yusiwen
 *
 * @param <K>
 *            Key
 * @param <S>
 *            Source value
 * @param <T>
 *            Target value
 */
public final class AdapterMap<K, S, T> extends AbstractMap<K, T> {

    /**
     * Source map
     */
    private final Map<K, S> src;
    /**
     * Entries
     */
    private final Set<Entry<K, T>> entries;

    public AdapterMap(Map<K, S> src, Function<S, T> function) {
        this.src = src;
        this.entries = new AdapterSet(src.entrySet(),
                (Function<Entry<K, S>, Entry<K, T>>) e -> new SimpleEntry(e.getKey(), function.apply(e.getValue())));
    }

    @Override
    public Set<Entry<K, T>> entrySet() {
        return entries;
    }

    @Override
    public int size() {
        return src.size();
    }
}