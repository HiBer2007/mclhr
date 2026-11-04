package com.hibernet.mclhr;

import com.hibernet.mclhr.ext.javafx.JavaFxSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 命令行入口：解析清单或 JavaFX 库，输出路径（每行一个 / classpath 一行）。 */
public final class MclhrCli {
    private static final String VERSION = "0.2.0";

    private MclhrCli() {
    }

    public static void main(String[] args) {
        try {
            System.exit(run(args));
        } catch (MclhrException e) {
            System.err.println("mclhr: " + e.getMessage());
            System.exit(2);
        }
    }

    private static int run(String[] args) throws MclhrException {
        if (args.length == 0) {
            usage();
            return 1;
        }

        String command = args[0];
        List<Path> stores = new ArrayList<>();
        List<String> coordinates = new ArrayList<>();
        Path manifestFile = null;
        Path hmclHome = null;
        boolean allowMissing = false;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--store" -> stores.add(Path.of(requireValue(args, ++i, "--store")));
                case "--manifest" -> manifestFile = Path.of(requireValue(args, ++i, "--manifest"));
                case "--library" -> coordinates.add(requireValue(args, ++i, "--library"));
                case "--hmcl-home" -> hmclHome = Path.of(requireValue(args, ++i, "--hmcl-home"));
                case "--allow-missing" -> allowMissing = true;
                case "--help", "-h" -> {
                    usage();
                    return 0;
                }
                default -> throw new MclhrException("未知参数: " + args[i]);
            }
        }

        return switch (command) {
            case "version" -> {
                System.out.println(VERSION);
                yield 0;
            }
            case "resolve" -> {
                List<Path> paths = resolve(manifestFile, coordinates, stores, allowMissing);
                paths.forEach(path -> System.out.println(path.toAbsolutePath()));
                yield 0;
            }
            case "classpath" -> {
                List<Path> paths = resolve(manifestFile, coordinates, stores, allowMissing);
                System.out.println(String.join(java.io.File.pathSeparator,
                        paths.stream().map(path -> path.toAbsolutePath().toString()).toList()));
                yield 0;
            }
            case "javafx" -> {
                List<Path> paths = JavaFxSupport.resolve(
                        stores.isEmpty() ? null : stores.get(0), hmclHome);
                paths.forEach(path -> System.out.println(path.toAbsolutePath()));
                yield 0;
            }
            default -> throw new MclhrException("未知命令: " + command);
        };
    }

    private static List<Path> resolve(Path manifestFile, List<String> coordinates,
                                      List<Path> stores, boolean allowMissing)
            throws MclhrException {
        if (manifestFile == null && coordinates.isEmpty()) {
            throw new MclhrException("需要 --manifest 或 --library");
        }
        Mclhr.Builder builder = Mclhr.builder().stores(stores);
        if (allowMissing) {
            builder.allowMissing();
        }
        Mclhr mclhr = builder.build();

        List<Path> resolved = new ArrayList<>();
        if (manifestFile != null) {
            if (!Files.isRegularFile(manifestFile)) {
                throw new MclhrException("清单不存在: " + manifestFile);
            }
            try {
                String json = Files.readString(manifestFile);
                resolved.addAll(mclhr.resolve(MclhrManifest.parse(json)));
            } catch (java.io.IOException e) {
                throw new MclhrException("读取清单失败: " + manifestFile, e);
            }
        }
        if (!coordinates.isEmpty()) {
            resolved.addAll(mclhr.resolveCoordinates(coordinates));
        }
        return resolved;
    }

    private static String requireValue(String[] args, int index, String option)
            throws MclhrException {
        if (index >= args.length) {
            throw new MclhrException(option + " 缺少值");
        }
        return args[index];
    }

    private static void usage() {
        System.err.println("""
                mclhr — generic classpath & shared library runtime

                用法:
                  java -jar mclhr.jar resolve   [--manifest <mclhr.json>] [--library <group:artifact:version[:classifier]> ...] --store <dir> [--store ...] [--allow-missing]
                  java -jar mclhr.jar classpath [--manifest <mclhr.json>] [--library <coord> ...] --store <dir> [--store ...]
                  java -jar mclhr.jar javafx    --store <dir> [--hmcl-home <dir>]
                  java -jar mclhr.jar version
                """);
    }
}
