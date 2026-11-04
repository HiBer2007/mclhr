package com.hibernet.mclhr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 清单文件。
 *
 * <p>推荐格式：</p>
 * <pre>{@code
 * {
 *   "modules": [
 *     {
 *       "id": "game",
 *       "libraries": [
 *         {"group": "org.example", "artifact": "engine", "version": "1.0"}
 *       ]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p>兼容格式：顶层直接写 {@code "libraries"}，会视为一个名为 {@code default} 的模块。</p>
 */
public final class MclhrManifest {
    private final List<MclhrModule> modules;

    public MclhrManifest(List<MclhrModule> modules) {
        this.modules = List.copyOf(modules);
    }

    public List<MclhrModule> modules() {
        return modules;
    }

    /** 所有模块的库按声明顺序展开。 */
    public List<MclhrLibrary> libraries() {
        List<MclhrLibrary> libraries = new ArrayList<>();
        for (MclhrModule module : modules) {
            libraries.addAll(module.libraries());
        }
        return libraries;
    }

    public List<MclhrLibrary> libraries(String moduleId) {
        for (MclhrModule module : modules) {
            if (module.id().equals(moduleId)) {
                return module.libraries();
            }
        }
        return List.of();
    }

    public static MclhrManifest parse(String json) throws MclhrException {
        Object root = Json.parse(json);
        if (!(root instanceof Map<?, ?> map)) {
            throw new MclhrException("清单根节点必须是对象");
        }

        Object rawModules = map.get("modules");
        if (rawModules instanceof List<?> list) {
            List<MclhrModule> modules = new ArrayList<>(list.size());
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> object)) {
                    throw new MclhrException("modules 元素必须是对象");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) object;
                modules.add(MclhrModule.fromJson(typed));
            }
            return new MclhrManifest(modules);
        }

        Object rawLibraries = map.get("libraries");
        if (rawLibraries instanceof List<?> list) {
            List<MclhrLibrary> libraries = new ArrayList<>(list.size());
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> object)) {
                    throw new MclhrException("libraries 元素必须是对象");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) object;
                libraries.add(MclhrLibrary.fromJson(typed));
            }
            return new MclhrManifest(List.of(new MclhrModule("default", libraries, false)));
        }

        throw new MclhrException("清单需要 modules 或 libraries 数组");
    }
}
