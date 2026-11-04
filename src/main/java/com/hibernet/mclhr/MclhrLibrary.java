package com.hibernet.mclhr;

import java.util.Map;
import java.util.Objects;

/**
 * 一个库坐标。
 *
 * <p>扁平文件名规则：{@code group_artifact_version[_classifier].jar}，
 * group 中的 '.' 替换为 '_'，例如
 * {@code org.openjfx:javafx-base:17.0.13:win} →
 * {@code org_openjfx_javafx-base_17.0.13-win.jar}。</p>
 */
public record MclhrLibrary(String group, String artifact, String version,
                           String classifier, String sha1) {

    public MclhrLibrary {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(version, "version");
    }

    public String coordinate() {
        return group + ":" + artifact + ":" + version
                + (classifier == null || classifier.isBlank() ? "" : ":" + classifier);
    }

    public String fileName() {
        String base = group.replace('.', '_') + "_" + artifact + "_" + version;
        if (classifier != null && !classifier.isBlank()) {
            base += "-" + classifier;
        }
        return base + ".jar";
    }

    public static MclhrLibrary fromJson(Map<String, Object> object) throws MclhrException {
        String group = string(object, "group");
        String artifact = string(object, "artifact");
        String version = string(object, "version");
        if (group == null || artifact == null || version == null) {
            throw new MclhrException("library 缺少 group/artifact/version: " + object);
        }
        String classifier = string(object, "classifier");
        String sha1 = string(object, "sha1");
        return new MclhrLibrary(group, artifact, version, classifier, sha1);
    }

    private static String string(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value instanceof String s && !s.isBlank() ? s : null;
    }
}
