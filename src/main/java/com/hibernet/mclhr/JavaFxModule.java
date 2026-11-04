package com.hibernet.mclhr;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** JavaFX 模块解析：共享库仓优先，旧版 HMCL 缓存作为回退。 */
public final class JavaFxModule {
    private static final Pattern JAVAFX_JAR = Pattern.compile(
            "^(?:org_openjfx_)?javafx-(base|graphics|controls)[-_].*\\.jar$");

    private JavaFxModule() {
    }

    /**
     * 解析 JavaFX jar。
     *
     * @param store   共享库仓（可为 null）
     * @param hmclHome HMCL 数据目录（可为 null，用于旧缓存回退）
     */
    public static List<Path> resolve(Path store, Path hmclHome) throws MclhrException {
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

        if (jars.isEmpty() && hmclHome != null && Files.isDirectory(hmclHome)) {
            jars.addAll(scanHmclCache(hmclHome));
        }

        if (jars.isEmpty()) {
            throw new MclhrException("未找到 JavaFX jar（store=" + store
                    + "，hmclHome=" + hmclHome + "）");
        }
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
