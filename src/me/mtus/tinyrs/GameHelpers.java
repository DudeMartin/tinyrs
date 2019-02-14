package me.mtus.tinyrs;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.mtus.tinyrs.utils.VersionUtility;

final class GameHelpers {

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("<param name=\"(.*?)\" value=\"(.*?)\">");

    static boolean isValidWorld(String number) {
        if (number.isEmpty()) {
            return false;
        }
        try {
            InetAddress.getByName("oldschool" + number + ".runescape.com");
            return true;
        } catch (UnknownHostException expected) {
            return false;
        }
    }

    static boolean isLatestRevision(File gamepackFile) {
        int revision;
        try {
            revision = VersionUtility.getRevision(new JarFile(gamepackFile));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(InetAddress.getByName("oldschool2.runescape.com"), 43594));
            DataOutputStream revisionStream = new DataOutputStream(socket.getOutputStream());
            revisionStream.write(15);
            revisionStream.writeInt(revision);
            return socket.getInputStream().read() == 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    static Map<String, String> parseParameters(String pageSource) {
        Map<String, String> parameters = new HashMap<String, String>();
        for (Matcher matcher = PARAMETER_PATTERN.matcher(pageSource);
             matcher.find();
             parameters.put(matcher.group(1), matcher.group(2)));
        return parameters;
    }
}