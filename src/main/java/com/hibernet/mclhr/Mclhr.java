package com.hibernet.mclhr;

import java.nio.file.Path;
import java.util.List;

/**
 * mclhr 的对外门面。
 *
 * <pre>{@code
 * Mclhr mclhr = Mclhr.builder()
 *         .store(Path.of("shared-libraries"))
 *         .store(Path.of("instance-libraries"))
 *         .build();
 *
 * List<Path> jars = mclhr.resolve(MclhrManifest.parse(json));
 * String classpath = mclhr.classpath(manifest);
 * }</pre>
 *
 * <p>核心只负责「坐标 → 文件」，不依赖 JavaFX、Fabric 或任何启动器。</p>
 */
public final class Mclhr {
    private final MclhrResolver resolver;

    private Mclhr(MclhrResolver resolver) {
        this.resolver = resolver;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Path> resolve(MclhrManifest manifest) throws MclhrException {
        return resolver.resolveAll(manifest.libraries());
    }

    public List<Path> resolveModule(MclhrManifest manifest, String moduleId) throws MclhrException {
        return resolver.resolveAll(manifest.libraries(moduleId));
    }

    public List<Path> resolveLibraries(List<MclhrLibrary> libraries) throws MclhrException {
        return resolver.resolveAll(libraries);
    }

    /** 直接按坐标字符串解析，例如 {@code group:artifact:version[:classifier]}。 */
    public List<Path> resolveCoordinates(List<String> coordinates) throws MclhrException {
        List<MclhrLibrary> libraries = new java.util.ArrayList<>(coordinates.size());
        for (String coordinate : coordinates) {
            libraries.add(MclhrLibrary.parse(coordinate));
        }
        return resolver.resolveAll(libraries);
    }

    /** 单坐标便捷方法。 */
    public Path resolveCoordinate(String coordinate) throws MclhrException {
        return resolver.resolve(MclhrLibrary.parse(coordinate));
    }

    public String classpath(MclhrManifest manifest) throws MclhrException {
        return join(resolve(manifest));
    }

    public String classpath(MclhrManifest manifest, String separator) throws MclhrException {
        return String.join(separator, resolve(manifest).stream().map(Path::toString).toList());
    }

    private static String join(List<Path> jars) {
        return String.join(java.io.File.pathSeparator,
                jars.stream().map(Path::toString).toList());
    }

    public static final class Builder {
        private final java.util.List<Path> stores = new java.util.ArrayList<>();
        private boolean failOnMissing = true;

        public Builder store(Path store) {
            if (store != null) {
                stores.add(store);
            }
            return this;
        }

        public Builder stores(List<Path> stores) {
            if (stores != null) {
                for (Path store : stores) {
                    store(store);
                }
            }
            return this;
        }

        public Builder allowMissing() {
            this.failOnMissing = false;
            return this;
        }

        public Mclhr build() {
            return new Mclhr(new MclhrResolver(stores, failOnMissing));
        }
    }
}
