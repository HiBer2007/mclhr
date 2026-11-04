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

    /** 读取清单并加载（无进度）。 */
    public static int loadManifest(Path manifestFile,
                                   List<Path> stores,
                                   JarConsumer consumer) throws MclhrException {
        return loadManifest(manifestFile, stores, consumer, MclhrProgress.NONE);
    }

    /** 读取清单并加载，向 progress 报告每一步。 */
    public static int loadManifest(Path manifestFile,
                                   List<Path> stores,
                                   JarConsumer consumer,
                                   MclhrProgress progress) throws MclhrException {
        if (!Files.isRegularFile(manifestFile)) {
            throw new MclhrException("清单不存在: " + manifestFile);
        }
        try {
            progress.onProgress("mclhr 解析清单", 0);
            MclhrManifest manifest = MclhrManifest.parse(Files.readString(manifestFile));
            Mclhr mclhr = Mclhr.builder().stores(stores).build();
            List<Path> jars = mclhr.resolve(manifest);
            progress.onProgress("mclhr 已解析 " + jars.size() + " 个库", 10);

            int loaded = 0;
            for (Path jar : jars) {
                progress.onProgress("mclhr 加载 " + jar.getFileName(),
                        10 + (jars.isEmpty() ? 0 : loaded * 90 / jars.size()));
                consumer.accept(jar);
                loaded++;
            }
            progress.onProgress("mclhr 完成", 100);
            return loaded;
        } catch (IOException e) {
            throw new MclhrException("读取清单失败: " + manifestFile, e);
        } catch (MclhrException e) {
            throw e;
        } catch (Exception e) {
            throw new MclhrException("加载清单失败: " + manifestFile, e);
        }
    }

    /** 按坐标列表加载（无进度）。 */
    public static int loadCoordinates(List<String> coordinates,
                                      List<Path> stores,
                                      JarConsumer consumer) throws MclhrException {
        return loadCoordinates(coordinates, stores, consumer, MclhrProgress.NONE);
    }

    /** 按坐标列表加载，向 progress 报告每一步。 */
    public static int loadCoordinates(List<String> coordinates,
                                      List<Path> stores,
                                      JarConsumer consumer,
                                      MclhrProgress progress) throws MclhrException {
        progress.onProgress("mclhr 解析坐标", 0);
        Mclhr mclhr = Mclhr.builder().stores(stores).build();
        List<Path> jars = mclhr.resolveCoordinates(coordinates);
        progress.onProgress("mclhr 已解析 " + jars.size() + " 个库", 10);

        int loaded = 0;
        for (Path jar : jars) {
            progress.onProgress("mclhr 加载 " + jar.getFileName(),
                    10 + (jars.isEmpty() ? 0 : loaded * 90 / jars.size()));
            try {
                consumer.accept(jar);
            } catch (Exception e) {
                throw new MclhrException("加载失败: " + jar, e);
            }
            loaded++;
        }
        progress.onProgress("mclhr 完成", 100);
        return loaded;
    }
}
