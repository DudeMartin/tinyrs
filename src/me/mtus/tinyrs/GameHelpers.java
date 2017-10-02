package me.mtus.tinyrs;

import org.classy.ClassFile;
import org.classy.MethodMember;
import org.classy.instructions.Instruction;
import org.classy.instructions.PushInstruction;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GameHelpers {

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("<param name=\"(.*?)\" value=\"(.*?)\">");

    static boolean isLatestRevision(File gamepackFile) {
        JarFile gamepack;
        try {
            gamepack = new JarFile(gamepackFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        int revision = -1;
        try {
            archiveLoop:
            for (Enumeration<JarEntry> jarEntries = gamepack.entries(); jarEntries.hasMoreElements(); ) {
                JarEntry entry = jarEntries.nextElement();
                if (entry.getName().equals("client.class")) {
                    ClassFile clientClass;
                    try {
                        clientClass = new ClassFile(readStream(gamepack.getInputStream(entry)));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                    for (MethodMember method : clientClass.methods) {
                        List<Instruction> instructions = method.instructions;
                        for (int i = 0, end = instructions.size() - 2; i < end; i++) {
                            Instruction firstInstruction = instructions.get(i);
                            Instruction secondInstruction = instructions.get(i + 1);
                            if (firstInstruction.getOpcode() == Instruction.SIPUSH
                                    && secondInstruction.getOpcode() == Instruction.SIPUSH) {
                                PushInstruction firstPush = (PushInstruction) firstInstruction;
                                PushInstruction secondPush = (PushInstruction) secondInstruction;
                                if (firstPush.value == 765 && secondPush.value == 503) {
                                    Instruction nextInstruction = instructions.get(i + 2);
                                    if (nextInstruction instanceof PushInstruction) {
                                        PushInstruction revisionInstruction = (PushInstruction) nextInstruction;
                                        revision = revisionInstruction.value;
                                        break archiveLoop;
                                    }
                                }
                            }
                        }
                    }
                    return false;
                }
            }
        } finally {
            try {
                gamepack.close();
            } catch (IOException ignored) {}
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