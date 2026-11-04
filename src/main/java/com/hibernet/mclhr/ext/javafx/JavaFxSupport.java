package com.hibernet.mclhr.ext.javafx;

import com.hibernet.mclhr.MclhrException;
import com.hibernet.mclhr.MclhrLibrary;
import com.hibernet.mclhr.MclhrProgress;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 可选扩展：JavaFX 库解析。
 *
 * <p>mclhr 核心不依赖 JavaFX；只有需要处理 JavaFX 的启动器才使用本类。</p>
 *
 * <p>查找顺序：共享库仓 → 旧版 HMCL 缓存（迁移期兼容）。</p>
 */
public final class JavaFxSupport {
    private static final Pattern JAVAFX_JAR = Pattern.compile(
            "^(?:org_openjfx_)?javafx-(base|graphics|controls)[-_].*\\.jar$");

    private JavaFxSupport() {
    }

    /** JavaFX 三个标准模块的坐标（便于写入 mclhr.json）。 */
    public static List<MclhrLibrary> coordinates(String version, String classifier) {
        return List.of(
                new MclhrLibrary("org.openjfx", "javafx-base", version, classifier, null),
                new MclhrLibrary("org.openjfx", "javafx-graphics", version, classifier, null),
                new MclhrLibrary("org.openjfx", "javafx-controls", version, classifier, null));
    }

    /**
     * 解析 JavaFX jar。
     *
     * @param store           共享库仓（可为 null）
     * @param legacyCacheHome 旧版 HMCL 数据目录（可为 null）
     */
    public static List<Path> resolve(Path store, Path legacyCacheHome) throws MclhrException {
        return resolve(store, legacyCacheHome, MclhrProgress.NONE);
    }

    public static List<Path> resolve(Path store, Path legacyCacheHome,
                                     MclhrProgress progress) throws MclhrException {
        progress.onProgress("mclhr 解析 JavaFX", 0);
        List<Path> jars = new ArrayList<>();

        if (store != null && Files.isDirectory(store)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(store, "*.jar")) {
                for (Path jar : stream) {
                    if (isJavaFxJar(jar.getFileName().toString())) {
                        jars.add(jar);
                    }
                }
            } catch (IOException e) {
                throw new MclhrException("扫描 JavaFX store 失败: " + store, e);
            }
        }

        if (jars.isEmpty() && legacyCacheHome != null && Files.isDirectory(legacyCacheHome)) {
            jars.addAll(scanHmclCache(legacyCacheHome));
        }

        if (jars.isEmpty()) {
            throw new MclhrException("未找到 JavaFX jar（store=" + store
                    + "，legacyCache=" + legacyCacheHome + "）");
        }
        progress.onProgress("mclhr 已找到 " + jars.size() + " 个 JavaFX 库", 100);
        jars.sort((a, b) -> a.getFileName().toString()
                .compareToIgnoreCase(b.getFileName().toString()));
        return jars;
    }

    static boolean isJavaFxJar(String fileName) {
        if (fileName.endsWith("-sources.jar") || fileName.endsWith("-javadoc.jar")) {
            return false;
        }
        return JAVAFX_JAR.matcher(fileName).matches();
    }

    private static List<Path> scanHmclCache(Path hmclHome) {
        List<Path> jars = new ArrayList<>();
        Path dependencies = hmclHome.resolve("dependencies");
        if (!Files.isDirectory(dependencies)) {
            return jars;
        }
        try (DirectoryStream<Path> platforms = Files.newDirectoryStream(dependencies)) {
            for (Path platform : platforms) {
                Path openjfx = platform.resolve("openjfx");
                if (!Files.isDirectory(openjfx)) {
                    continue;
                }
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(openjfx, "javafx-*.jar")) {
                    for (Path jar : stream) {
                        if (isJavaFxJar(jar.getFileName().toString())) {
                            jars.add(jar);
                        }
                    }
                } catch (IOException ignored) {
                    // 跳过不可读目录
                }
            }
        } catch (IOException ignored) {
            // 没有缓存
        }
        return jars;
    }
}
