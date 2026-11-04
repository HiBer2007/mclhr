package com.hibernet.mclhr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 自测：Loader 的 consumer 顺序与数量。 */
public final class LoaderSelfTest {
    private LoaderSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("mclhr-loader-test");
        try {
            Path store = Files.createDirectory(root.resolve("store"));
            Files.writeString(store.resolve("org_example_engine_1.0.jar"), "");
            Files.writeString(store.resolve("org_example_ui_1.0.jar"), "");

            String manifest = """
                    {"modules":[{"id":"game","libraries":[
                      {"group":"org.example","artifact":"engine","version":"1.0"},
                      {"group":"org.example","artifact":"ui","version":"1.0"}
                    ]}]}
                    """;
            Path manifestFile = Files.writeString(root.resolve("mclhr.json"), manifest);

            StringBuilder loaded = new StringBuilder();
            int count = MclhrLoader.loadManifest(manifestFile, List.of(store), jar -> {
                loaded.append(jar.getFileName()).append(";");
                if (!Files.isRegularFile(jar)) {
                    throw new IllegalStateException("not a file: " + jar);
                }
            });

            if (count != 2 || !loaded.toString().contains("org_example_engine_1.0.jar")) {
                throw new AssertionError("bad load result: " + count + " " + loaded);
            }
            System.out.println("mclhr loader self-test OK");
        } finally {
            try (var stream = Files.walk(root)) {
                stream.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }
}
