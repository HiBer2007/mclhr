package com.hibernet.mclhr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 无依赖自测：解析、优先级、optional、SHA-1 与坐标字符串。 */
public final class SelfTest {
    private SelfTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("mclhr-selftest");
        try {
            Path global = Files.createDirectory(root.resolve("global"));
            Path instance = Files.createDirectory(root.resolve("instance"));

            touch(global, "org_example_engine_1.0.jar");
            Path ui = touch(instance, "org_example_ui_1.0.jar");
            Path override = touch(instance, "org_example_engine_1.0.jar");

            String json = """
                    {
                      "modules": [
                        {
                          "id": "game",
                          "libraries": [
                            {"group": "org.example", "artifact": "engine", "version": "1.0"},
                            {"group": "org.example", "artifact": "ui", "version": "1.0"},
                            {"group": "org.example", "artifact": "optional", "version": "1.0", "optional": true}
                          ]
                        }
                      ]
                    }
                    """;

            Mclhr mclhr = Mclhr.builder().store(instance).store(global).build();
            List<Path> resolved = mclhr.resolve(MclhrManifest.parse(json));

            check(resolved.size() == 2, "optional 应被跳过，实际 " + resolved.size());
            check(resolved.get(0).equals(override), "实例仓应优先于全局仓");
            check(resolved.get(1).equals(ui), "ui 应来自实例仓");

            // 坐标字符串
            MclhrLibrary parsed = MclhrLibrary.parse("org.example:engine:1.0");
            check(parsed.fileName().equals("org_example_engine_1.0.jar"),
                    "坐标 → 文件名错误: " + parsed.fileName());
            List<Path> viaCoordinate = mclhr.resolveCoordinates(List.of("org.example:engine:1.0"));
            check(viaCoordinate.size() == 1 && viaCoordinate.get(0).equals(override),
                    "坐标解析结果错误: " + viaCoordinate);

            // SHA-1 校验
            String sha1 = MclhrResolver.sha1(override);
            mclhr.resolveCoordinates(List.of("org.example:engine:1.0@" + sha1));

            try {
                mclhr.resolveCoordinates(List.of("org.example:engine:1.0@0000000000000000000000000000000000000000"));
                throw new AssertionError("错误 SHA-1 应抛异常");
            } catch (MclhrException expected) {
                // ok
            }

            // optional 缺失时 allowMissing 的语义
            Mclhr allowMissing = Mclhr.builder().store(global).allowMissing().build();
            check(allowMissing.resolveCoordinates(List.of("org.example:missing:1.0")).isEmpty(),
                    "allowMissing 应跳过缺失库");

            System.out.println("mclhr self-test OK");
        } finally {
            deleteRecursively(root);
        }
    }

    private static Path touch(Path directory, String name) throws Exception {
        return Files.writeString(directory.resolve(name), "");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void deleteRecursively(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // 清理失败不影响测试结果
                }
            });
        }
    }
}
