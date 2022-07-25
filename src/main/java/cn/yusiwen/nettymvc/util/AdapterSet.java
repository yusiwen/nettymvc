package cn.yusiwen.nettymvc.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

/**
 * @author yusiwen
 * @param <S>
 *            Source set
 * @param <T>
 *            Iterator
 */
public final class AdapterSet<S, T> extends AbstractSet<T> {

    /**
     * Source set
     */
    private final Set<S> src;
    /**
     * Iterator
     */
    private final Iterator<T> iterator;

    public AdapterSet(Set<S> src, Function<S, T> function) {
        this.src = src;
        this.iterator = new Iterator<T>() {

            private final Function<S, T> f = function;
            private final Iterator<S> it = src.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return f.apply(it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

    @Override
    public int size() {
        return src.size();
    }
}