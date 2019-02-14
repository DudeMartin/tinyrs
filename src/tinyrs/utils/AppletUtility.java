package tinyrs.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppletUtility {

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("<param name=\"(.*?)\" value=\"(.*?)\">");

    private AppletUtility() {

    }

    public static String getHostForWorld(int world) {
        return "oldschool" + world + ".runescape.com";
    }

    public static boolean isValidWorld(int world) {
        try {
            InetAddress.getByName(getHostForWorld(world));
            return true;
        } catch (UnknownHostException expected) {
            return false;
        }
    }

    public static Map<String, String> parseParameters(String pageSource) {
        Map<String, String> parameters = new HashMap<String, String>();
        Matcher matcher = PARAMETER_PATTERN.matcher(pageSource);
        while (matcher.find()) {
            parameters.put(matcher.group(1), matcher.group(2));
        }
        return parameters;
    }
}
