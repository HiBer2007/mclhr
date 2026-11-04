package com.hibernet.mclhr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/** 在多个 store 中解析库坐标。 */
public final class MclhrResolver {
    private MclhrResolver() {
    }

    /**
     * 按 store 顺序查找库；命中后校验 SHA-1（若清单提供）。
     *
     * @throws MclhrException 找不到或校验失败
     */
    public static Path resolve(MclhrLibrary library, List<Path> stores) throws MclhrException {
        String fileName = library.fileName();
        for (Path store : stores) {
            if (store == null) {
                continue;
            }
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
        throw new MclhrException("找不到库 " + library.coordinate()
                + "（文件名 " + fileName + "，stores=" + stores + "）");
    }

    public static List<Path> resolveAll(List<MclhrLibrary> libraries, List<Path> stores)
            throws MclhrException {
        List<Path> resolved = new ArrayList<>(libraries.size());
        for (MclhrLibrary library : libraries) {
            resolved.add(resolve(library, stores));
        }
        return resolved;
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
