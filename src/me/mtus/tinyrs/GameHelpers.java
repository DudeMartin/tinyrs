package me.mtus.tinyrs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GameHelpers {

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("<param name=\"(.*?)\" value=\"(.*?)\">");

    static boolean isLatestRevision(File gamepackFile) {
        return false;
    }

    static byte[] readStream(InputStream stream) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int bytesRead;
             (bytesRead = stream.read(buffer)) >= 0;
             byteStream.write(buffer, 0, bytesRead));
        return byteStream.toByteArray();
    }

    static Map<String, String> parseParameters(String pageSource) {
        Map<String, String> parameters = new HashMap<String, String>();
        for (Matcher matcher = PARAMETER_PATTERN.matcher(pageSource);
             matcher.find();
             parameters.put(matcher.group(1), matcher.group(2)));
        return parameters;
    }
}