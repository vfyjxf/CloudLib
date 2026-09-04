package dev.vfyjxf.cloudlib.util;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.util.Namespace;

public final class CloudNamespaces {

    public static Namespace ofMod(String path) {
        return Namespace.of(Constants.modId, path);
    }

}
