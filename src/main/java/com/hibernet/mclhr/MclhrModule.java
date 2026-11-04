package com.hibernet.mclhr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 清单中的一个模块：一组库 + 可选标记。 */
public record MclhrModule(String id, List<MclhrLibrary> libraries, boolean optional) {

    public MclhrModule {
        libraries = List.copyOf(libraries);
    }

    public static MclhrModule fromJson(Map<String, Object> object) throws MclhrException {
        Object rawId = object.get("id");
        String id = rawId instanceof String s && !s.isBlank() ? s : "default";
        boolean optional = MclhrLibrary.bool(object, "optional", false);

        Object rawLibraries = object.get("libraries");
        if (!(rawLibraries instanceof List<?> list)) {
            throw new MclhrException("module '" + id + "' 缺少 libraries 数组");
        }

        List<MclhrLibrary> libraries = new ArrayList<>(list.size());
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new MclhrException("module '" + id + "' 的 libraries 元素必须是对象");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            libraries.add(MclhrLibrary.fromJson(typed));
        }
        return new MclhrModule(id, libraries, optional);
    }
}
