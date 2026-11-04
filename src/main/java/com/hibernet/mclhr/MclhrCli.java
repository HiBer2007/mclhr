package com.hibernet.mclhr;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 命令行入口：输出解析到的 jar 绝对路径，每行一个。 */
public final class MclhrCli {
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
        Path manifest = null;
        Path hmclHome = null;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--store" -> stores.add(Path.of(requireValue(args, ++i, "--store")));
                case "--manifest" -> manifest = Path.of(requireValue(args, ++i, "--manifest"));
                case "--hmcl-home" -> hmclHome = Path.of(requireValue(args, ++i, "--hmcl-home"));
                default -> throw new MclhrException("未知参数: " + args[i]);
            }
        }

        List<Path> resolved = switch (command) {
            case "javafx" -> MclhrRuntime.resolveJavaFx(
                    hmclHome, stores.isEmpty() ? null : stores.get(0));
            case "resolve" -> {
                if (manifest == null) {
                    throw new MclhrException("resolve 需要 --manifest");
                }
                yield MclhrRuntime.resolveManifest(manifest, stores);
            }
            default -> throw new MclhrException("未知命令: " + command);
        };

        for (Path path : resolved) {
            System.out.println(path.toAbsolutePath());
        }
        return 0;
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
                mclhr — multiver class loader handle runtime

                用法:
                  java -jar mclhr.jar javafx --store <libraries> [--hmcl-home <dir>]
                  java -jar mclhr.jar resolve --manifest <mclhr.json> --store <dir> [--store ...]
                """);
    }
}
