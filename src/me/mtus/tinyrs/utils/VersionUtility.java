package me.mtus.tinyrs.utils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class VersionUtility {

    private static final byte[] REVISION_NUMBER_PATTERN = new byte[] { 17, 2, -3, 17, 1, -9, 17 };

    private VersionUtility() {

    }

    public static int getRevision(JarFile gamepack) throws IOException {
        Enumeration<JarEntry> entries = gamepack.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!"client.class".equals(entry.getName())) {
                continue;
            }
            byte[] classBytes = StreamUtility.readBytes(gamepack.getInputStream(entry));
            int revisionPatternIndex = indexOf(classBytes, REVISION_NUMBER_PATTERN);
            if (revisionPatternIndex == -1) {
                return -1;
            }
            int revisionOffset = revisionPatternIndex + REVISION_NUMBER_PATTERN.length;
            return ((classBytes[revisionOffset] << 8) | classBytes[revisionOffset + 1]) & 0xff;
        }
        return -1;
    }

    private static int indexOf(byte[] source, byte[] pattern) {
        int sourceLength = source.length;
        int patternLength = pattern.length;
        sourceLoop: for (int i = 0; i < sourceLength; i++) {
            for (int j = 0; j < patternLength; j++) {
                if (source[i + j] != pattern[j]) {
                    continue sourceLoop;
                }
            }
            return i;
        }
        return -1;
    }
}
