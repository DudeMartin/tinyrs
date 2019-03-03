package tinyrs.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class StreamUtility {

    private StreamUtility() {

    }

    public static byte[] readBytes(final InputStream stream, final ProgressListener progressListener)
            throws IOException {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = stream.read(buffer)) > 0) {
            byteStream.write(buffer, 0, bytesRead);
            if (progressListener != null) {
                progressListener.onBytesRead(bytesRead);
            }
        }
        return byteStream.toByteArray();
    }

    public static byte[] readBytes(final InputStream stream) throws IOException {
        return readBytes(stream, null);
    }

    public interface ProgressListener {

        void onBytesRead(int amount);
    }
}
