package com.hibernet.mclhr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** {@code mclhr.json} 清单。 */
public final class MclhrManifest {
    private final List<MclhrLibrary> libraries;

    public MclhrManifest(List<MclhrLibrary> libraries) {
        this.libraries = List.copyOf(libraries);
    }

    public List<MclhrLibrary> libraries() {
        return libraries;
    }

    public static MclhrManifest parse(String json) throws MclhrException {
        Object root = Json.parse(json);
        if (!(root instanceof Map<?, ?> map)) {
            throw new MclhrException("清单根节点必须是对象");
        }
        Object rawLibraries = map.get("libraries");
        if (!(rawLibraries instanceof List<?> list)) {
            throw new MclhrException("清单缺少 libraries 数组");
        }

        List<MclhrLibrary> libraries = new ArrayList<>(list.size());
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> object)) {
                throw new MclhrException("libraries 元素必须是对象");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) object;
            libraries.add(MclhrLibrary.fromJson(typed));
        }
        return new MclhrManifest(libraries);
    }
}
