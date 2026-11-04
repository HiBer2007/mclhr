package com.hibernet.mclhr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 把清单里的库加载到宿主的 classpath。
 *
 * <p>mclhr 只负责解析与调用 {@link JarConsumer}；实际注入方式（Agent 的
 * {@code Instrumentation}、URLClassLoader 等）由宿主决定。</p>
 */
public final class MclhrLoader {

    /** 宿主提供的 jar 消费者。 */
    @FunctionalInterface
    public interface JarConsumer {
        void accept(Path jar) throws Exception;
    }

    private MclhrLoader() {
    }

    /**
     * 读取清单，在多个 store 中解析并逐个交给 consumer。
     *
     * @return 成功加载的 jar 数量
     */
    public static int loadManifest(Path manifestFile,
                                   List<Path> stores,
                                   JarConsumer consumer) throws MclhrException {
        if (!Files.isRegularFile(manifestFile)) {
            throw new MclhrException("清单不存在: " + manifestFile);
        }
        try {
            MclhrManifest manifest = MclhrManifest.parse(Files.readString(manifestFile));
            Mclhr mclhr = Mclhr.builder().stores(stores).build();
            List<Path> jars = mclhr.resolve(manifest);

            int loaded = 0;
            for (Path jar : jars) {
                try {
                    consumer.accept(jar);
                    loaded++;
                } catch (Exception e) {
                    throw new MclhrException("加载失败: " + jar, e);
                }
            }
            return loaded;
        } catch (IOException e) {
            throw new MclhrException("读取清单失败: " + manifestFile, e);
        }
    }

    /** 按坐标列表加载。 */
    public static int loadCoordinates(List<String> coordinates,
                                      List<Path> stores,
                                      JarConsumer consumer) throws MclhrException {
        Mclhr mclhr = Mclhr.builder().stores(stores).build();
        List<Path> jars = mclhr.resolveCoordinates(coordinates);
        int loaded = 0;
        for (Path jar : jars) {
            try {
                consumer.accept(jar);
                loaded++;
            } catch (Exception e) {
                throw new MclhrException("加载失败: " + jar, e);
            }
        }
        return loaded;
    }
}
