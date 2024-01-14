package dev.vfyjxf.cloudlib.data.lang;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import javax.annotation.Nullable;

public class LangBuilder {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final MutableList<LangBuilder> builders = Lists.mutable.empty();

    private final MutableList<Lang> defines = Lists.mutable.empty();

    private final String modid;
    private final String specificNameSpace;
    private String key;
    /**
     * English located value.
     */
    private String value;

    public static LangBuilder create(String modid, @Nullable String specificNameSpace) {
        LangBuilder builder = new LangBuilder(modid, specificNameSpace);
        builders.add(builder);
        return builder;
    }

    public static LangBuilder create(String modid) {
        return create(modid, null);
    }

    public LangBuilder child(String extraNameSpace) {
        return create(modid, specificNameSpace == null ? extraNameSpace : specificNameSpace + "." + extraNameSpace);
    }

    private LangBuilder(String modid, @Nullable String specificNameSpace) {
        this.modid = modid;
        this.specificNameSpace = specificNameSpace;
    }

    public Lang define(String key, String value) {
        if (key == null || value == null) {
            LOGGER.error("key or value is null");
            return null;
        }
        String realKey = specificNameSpace == null || specificNameSpace.isEmpty() ? modid + "." + key : modid + "." + specificNameSpace + "." + key;
        Lang lang = new Lang(realKey, value);
        this.key = null;
        this.value = null;
        this.defines.add(lang);
        return lang;
    }

    public String defineKey(String key, String value) {
        return define(key, value).key();
    }

    public Lang define() {
        return this.define(key, value);
    }

    public LangBuilder key(String key) {
        this.key = key;
        return this;
    }

    public LangBuilder value(String value) {
        this.value = value;
        return this;
    }

    public MutableList<Lang> getDefines() {
        return defines;
    }
}
