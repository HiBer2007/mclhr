package com.hibernet.mclhr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 兼容入口（旧版 FLFHMCL Agent 使用）。
 *
 * <p>新代码请直接使用 {@link Mclhr}；这里保留静态方法只是为了避免破坏已有调用方。</p>
 */
@Deprecated
public final class MclhrRuntime {
    private MclhrRuntime() {
    }

    /** 解析 JavaFX jar（委托给可选扩展 {@code ext.javafx.JavaFxSupport}）。 */
    public static List<Path> resolveJavaFx(Path hmclHome, Path store) throws MclhrException {
        return com.hibernet.mclhr.ext.javafx.JavaFxSupport.resolve(store, hmclHome);
    }

    /** 按清单解析全部库。 */
    public static List<Path> resolveManifest(Path manifestFile, List<Path> stores)
            throws MclhrException {
        if (!Files.isRegularFile(manifestFile)) {
            throw new MclhrException("清单不存在: " + manifestFile);
        }
        try {
            String json = Files.readString(manifestFile);
            Mclhr mclhr = Mclhr.builder().stores(stores).build();
            return mclhr.resolve(MclhrManifest.parse(json));
        } catch (java.io.IOException e) {
            throw new MclhrException("读取清单失败: " + manifestFile, e);
        }
    }
}
