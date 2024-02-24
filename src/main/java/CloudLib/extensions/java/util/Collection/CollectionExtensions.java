package CloudLib.extensions.java.util.Collection;

import dev.vfyjxf.cloudlib.api.annotations.NotNullByDefault;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;

import java.util.Collection;

@Extension
@NotNullByDefault
public class CollectionExtensions {
    public static <E> MutableList<E> toList(@This Collection<E> $this) {
        if ($this instanceof MutableList) {
            return (MutableList<E>) $this;
        }
        return Lists.mutable.ofAll($this);
    }

    @SuppressWarnings("unchecked")
    public static <E> RichIterable<E> rich(@This Collection<E> $this) {
        if ($this instanceof RichIterable) {
            return (RichIterable<E>) $this;
        }
        return CollectionAdapter.adapt($this);
    }

    public static <E> RichIterable<E> rich(@This Iterable<E> $this) {
        if ($this instanceof RichIterable) {
            return (RichIterable<E>) $this;
        }
        if ($this instanceof Collection) {
            return rich((Collection<E>) $this);
        }
        if ($this instanceof Iterable) {
            return rich(Lists.immutable.ofAll($this));
        }
        throw new IllegalArgumentException("Unsupported type: " + $this.getClass());
    }

}