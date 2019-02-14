package me.mtus.tinyrs.utils;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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
            int patternIndex = indexOf(classBytes, REVISION_NUMBER_PATTERN);
            if (patternIndex == -1) {
                return -1;
            }
            DataInput classDataInput = new DataInputStream(new ByteArrayInputStream(classBytes));
            classDataInput.skipBytes(patternIndex + REVISION_NUMBER_PATTERN.length);
            return classDataInput.readShort();
        }
        return -1;
    }

    public static boolean isLatestRevision(int revision) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(InetAddress.getByName("oldschool2.runescape.com"), 43594));
            DataOutput socketStream = new DataOutputStream(socket.getOutputStream());
            socketStream.write(15);
            socketStream.writeInt(revision);
            return socket.getInputStream().read() == 0;
        } finally {
            socket.close();
        }
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
