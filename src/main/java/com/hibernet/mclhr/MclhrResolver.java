package com.hibernet.mclhr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/** 在多个 store 中解析库坐标（线程安全，可复用）。 */
public final class MclhrResolver {
    private final List<Path> stores;
    private final boolean failOnMissing;

    /**
     * @param stores       按优先级排列的库目录
     * @param failOnMissing 缺失（且非 optional）时是否抛异常
     */
    public MclhrResolver(List<Path> stores, boolean failOnMissing) {
        List<Path> copy = new ArrayList<>();
        for (Path store : stores) {
            if (store != null) {
                copy.add(store);
            }
        }
        this.stores = List.copyOf(copy);
        this.failOnMissing = failOnMissing;
    }

    public List<Path> stores() {
        return stores;
    }

    /**
     * 解析单个库。
     *
     * @throws MclhrException 找不到（非 optional 且 failOnMissing）或 SHA-1 不匹配
     */
    public Path resolve(MclhrLibrary library) throws MclhrException {
        Path found = find(library);
        if (found != null) {
            return found;
        }
        if (library.optional() || !failOnMissing) {
            return null;
        }
        throw new MclhrException("找不到库 " + library.coordinate()
                + "（文件名 " + library.fileName() + "，stores=" + stores + "）");
    }

    /** 按顺序解析；缺失且允许跳过时忽略。 */
    public List<Path> resolveAll(List<MclhrLibrary> libraries) throws MclhrException {
        List<Path> resolved = new ArrayList<>(libraries.size());
        for (MclhrLibrary library : libraries) {
            Path path = resolve(library);
            if (path != null) {
                resolved.add(path);
            }
        }
        return resolved;
    }

    private Path find(MclhrLibrary library) throws MclhrException {
        String fileName = library.fileName();
        for (Path store : stores) {
            Path candidate = store.resolve(fileName);
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            if (library.sha1() != null && !library.sha1().isBlank()) {
                String actual = sha1(candidate);
                if (!actual.equalsIgnoreCase(library.sha1())) {
                    throw new MclhrException("SHA-1 不匹配: " + candidate
                            + "（期望 " + library.sha1() + "，实际 " + actual + "）");
                }
            }
            return candidate;
        }
        return null;
    }

    public static String sha1(Path file) throws MclhrException {
        try (InputStream input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new MclhrException("计算 SHA-1 失败: " + file, e);
        }
    }
}
