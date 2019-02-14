package me.mtus.tinyrs.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class StreamUtility {

    private StreamUtility() {

    }

    public static byte[] readBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = stream.read(buffer)) > 0) {
            byteStream.write(buffer, 0, bytesRead);
        }
        return byteStream.toByteArray();
    }
}
