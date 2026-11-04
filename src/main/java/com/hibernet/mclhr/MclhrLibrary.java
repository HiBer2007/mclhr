package com.hibernet.mclhr;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 一个库坐标。
 *
 * <p>扁平文件名规则：{@code group_artifact_version[_classifier].jar}，
 * group 中的 '.' 替换为 '_'，例如
 * {@code org.openjfx:javafx-base:17.0.13:win} →
 * {@code org_openjfx_javafx-base_17.0.13-win.jar}。</p>
 *
 * <p>{@code optional=true} 的库在缺失时会被跳过，不会让整个解析失败。</p>
 */
public record MclhrLibrary(String group, String artifact, String version,
                           String classifier, String sha1, boolean optional) {

    public MclhrLibrary {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(version, "version");
    }

    public MclhrLibrary(String group, String artifact, String version,
                        String classifier, String sha1) {
        this(group, artifact, version, classifier, sha1, false);
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
        boolean optional = bool(object, "optional", false);
        return new MclhrLibrary(group, artifact, version, classifier, sha1, optional);
    }

    /**
     * 解析坐标字符串：
     * {@code group:artifact:version[:classifier][@sha1][?optional]}。
     * 例如 {@code org.example:engine:1.0}、
     * {@code org.openjfx:javafx-base:17.0.13:win}、
     * {@code org.example:engine:1.0@<sha1>?optional}。
     */
    public static MclhrLibrary parse(String value) throws MclhrException {
        if (value == null || value.isBlank()) {
            throw new MclhrException("坐标不能为空");
        }
        String text = value.trim();
        String sha1 = null;
        boolean optional = false;

        int at = text.indexOf('@');
        if (at >= 0) {
            sha1 = text.substring(at + 1).trim();
            text = text.substring(0, at);
        }
        int question = text.indexOf('?');
        if (question >= 0) {
            String flags = text.substring(question + 1);
            optional = List.of(flags.split(",")).contains("optional");
            text = text.substring(0, question);
        }

        String[] parts = text.split(":");
        if (parts.length < 3 || parts.length > 4) {
            throw new MclhrException("坐标格式应为 group:artifact:version[:classifier]: " + value);
        }
        String classifier = parts.length == 4 ? parts[3] : null;
        return new MclhrLibrary(parts[0], parts[1], parts[2], classifier, sha1, optional);
    }

    static String string(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value instanceof String s && !s.isBlank() ? s : null;
    }

    static boolean bool(Map<String, Object> object, String key, boolean fallback) {
        Object value = object.get(key);
        return value instanceof Boolean b ? b : fallback;
    }
}
