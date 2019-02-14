package me.mtus.tinyrs;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    static Map<String, String> parseParameters(String pageSource) {
        Map<String, String> parameters = new HashMap<String, String>();
        for (Matcher matcher = PARAMETER_PATTERN.matcher(pageSource);
             matcher.find();
             parameters.put(matcher.group(1), matcher.group(2)));
        return parameters;
    }
}