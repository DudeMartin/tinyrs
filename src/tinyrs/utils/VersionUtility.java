package tinyrs.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class VersionUtility {

    private static final byte[] REVISION_NUMBER_PATTERN = { 17, 2, -3, 17, 1, -9, 17 };

    private VersionUtility() {

    }

    public static int getRevision(final JarFile gamepack) throws IOException {
        for (final JarEntry entry : Collections.list(gamepack.entries())) {
            if (!"client.class".equals(entry.getName())) {
                continue;
            }
            final byte[] classBytes = StreamUtility.readBytes(gamepack.getInputStream(entry));
            final int patternIndex = indexOfSubArray(classBytes, REVISION_NUMBER_PATTERN);
            if (patternIndex == -1) {
                return -1;
            }
            final int revisionIndex = patternIndex + REVISION_NUMBER_PATTERN.length;
            return classBytes[revisionIndex] << 8 | classBytes[revisionIndex + 1] & 0xff;
        }
        return -1;
    }

    public static boolean isLatestRevision(final int revision) throws IOException {
        final Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(AppletUtility.getHostForWorld(2), 43594));
            final DataOutputStream socketStream = new DataOutputStream(socket.getOutputStream());
            socketStream.write(15);
            socketStream.writeInt(revision);
            socketStream.flush();
            return socket.getInputStream().read() == 0;
        } finally {
            socket.close();
        }
    }

    private static int indexOfSubArray(final byte[] source, final byte[] target) {
        final int sourceLength = source.length;
        final int patternLength = target.length;
        sourceLoop: for (int i = 0; i < sourceLength; i++) {
            for (int j = 0; j < patternLength; j++) {
                if (source[i + j] != target[j]) {
                    continue sourceLoop;
                }
            }
            return i;
        }
        return -1;
    }
}
