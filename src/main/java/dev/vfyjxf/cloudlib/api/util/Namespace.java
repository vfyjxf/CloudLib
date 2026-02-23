package dev.vfyjxf.cloudlib.api.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.vfyjxf.cloudlib.api.annotation.FieldNotNullByDefault;
import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.util.Checks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @see ResourceLocation
 * @see Path
 */
@NotNullByDefault
@FieldNotNullByDefault
public final class Namespace implements Comparable<Namespace> {

    public static final Codec<Namespace> codec = Codec.STRING.comapFlatMap(
            str -> {
                Namespace namespace = maybe(str);
                if (namespace == null) return DataResult.error(() -> "Invalid namespace: " + str);
                else return DataResult.success(namespace);
            },
            namespace -> namespace.root + ":" + namespace.path
    ).stable();

    public static final StreamCodec<ByteBuf, Namespace> streamCodec = ByteBufCodecs.STRING_UTF8.map(
            Namespace::parse,
            namespace -> namespace.root + ":" + namespace.path
    );

    public static Namespace ofMc(String path) {
        return new Namespace("minecraft", path);
    }

    public static Namespace of(String root, String path) {
        return new Namespace(root, path);
    }

    public static Namespace parse(String str) {
        int index = str.indexOf(":");
        if (index != -1) {
            return new Namespace(str.substring(0, index), str.substring(index + 1));
        } else {
            return new Namespace("minecraft", str);
        }
    }

    public static @Nullable Namespace maybe(String str) {
        int index = str.indexOf(":");
        if (index != -1) {
            String root = str.substring(0, index);
            String path = str.substring(index + 1);
            if (!ResourceLocation.isValidPath(path)) return null;
            return new Namespace(root, path);
        } else {
            return ResourceLocation.isValidPath(str) ? ofMc(str) : null;
        }
    }

    private final String root;
    private final String path;
    private final int hash;

    public String root() {
        return root;
    }

    public String path() {
        return path;
    }

    public int hash() {
        return hash;
    }

    private Namespace(String root, String path) {
        Checks.checkNotNull(root, "root");
        Checks.checkNotNull(path, "path");
        if (!ResourceLocation.isValidNamespace(root)) {
            throw new IllegalArgumentException("Non [a-z0-9_.-] character in root of namespace: " + root + ":" + path);
        }
        if (!ResourceLocation.isValidPath(path)) {
            throw new IllegalArgumentException("Non [a-z0-9/._-] character in path of namespace: " + root + ":" + path);
        }
        this.hash = 31 * root.hashCode() + path.hashCode();
        this.root = root;
        this.path = path;
    }

    public Namespace resolve(String path) {
        Checks.checkNotNull(path, "path");
        return new Namespace(root, this.path + "/" + path);
    }

    public Namespace withRoot(String root) {
        Checks.checkNotNull(root, "root");
        return new Namespace(root, path);
    }

    public Namespace withPath(String path) {
        Checks.checkNotNull(path, "path");
        return new Namespace(root, path);
    }

    public Namespace resolve(Namespace namespace) {
        Checks.checkNotNull(namespace, "namespace");
        return new Namespace(root, this.path + "/" + namespace.path);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Namespace namespace = (Namespace) o;
        return Objects.equals(root, namespace.root) && Objects.equals(path, namespace.path);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return root + ":" + path;
    }

    @Override
    public int compareTo(Namespace o) {
        int result = root.compareTo(o.root);
        return result != 0 ? result : path.compareTo(o.path);
    }
}
