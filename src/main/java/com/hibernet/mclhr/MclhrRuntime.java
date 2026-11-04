package com.hibernet.mclhr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * mclhr 对外入口。
 *
 * <p>只负责解析库路径；把 jar 追加到 classpath 由宿主（Agent / 启动器）决定。</p>
 */
public final class MclhrRuntime {
    private MclhrRuntime() {
    }

    /** 解析 JavaFX jar；store 优先，HMCL 缓存回退。 */
    public static List<Path> resolveJavaFx(Path hmclHome, Path store) throws MclhrException {
        return JavaFxModule.resolve(store, hmclHome);
    }

    /** 按清单解析全部库。 */
    public static List<Path> resolveManifest(Path manifestFile, List<Path> stores)
            throws MclhrException {
        if (!Files.isRegularFile(manifestFile)) {
            throw new MclhrException("清单不存在: " + manifestFile);
        }
        try {
            String json = Files.readString(manifestFile);
            return MclhrResolver.resolveAll(MclhrManifest.parse(json).libraries(), stores);
        } catch (java.io.IOException e) {
            throw new MclhrException("读取清单失败: " + manifestFile, e);
        }
    }
}
