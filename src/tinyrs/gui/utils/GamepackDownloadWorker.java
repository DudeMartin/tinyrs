package tinyrs.gui.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import tinyrs.GlobalProperty;
import tinyrs.utils.AppletUtility;
import tinyrs.utils.StreamUtility;

public class GamepackDownloadWorker extends SwingWorker<Void, Integer> {

    private final File destinationFile;
    private final JProgressBar progressBar;

    public GamepackDownloadWorker(final File destinationFile, final JProgressBar progressBar) {
        this.destinationFile = destinationFile;
        this.progressBar = progressBar;
    }

    @Override
    protected final Void doInBackground() throws Exception {
        final URL gamepackAddress = new URL(
                "http",
                AppletUtility.getHostForWorld(GlobalProperty.DEFAULT_WORLD.getDefault(int.class)),
                "/gamepack.jar");
        final URLConnection gamepackConnection = gamepackAddress.openConnection();
        final int gamepackSize = gamepackConnection.getContentLength();
        publish(gamepackSize == -1 ? Integer.MIN_VALUE : 0);
        final InputStream gamepackStream = gamepackConnection.getInputStream();
        try {
            final byte[] gamepackBytes;
            if (gamepackSize == -1) {
                gamepackBytes = StreamUtility.readBytes(gamepackStream);
            } else {
                final AtomicInteger totalBytesRead = new AtomicInteger();
                gamepackBytes = StreamUtility.readBytes(gamepackStream, new StreamUtility.ProgressListener() {

                    @Override
                    public void onBytesRead(final int amount) {
                        publish(100 * totalBytesRead.addAndGet(amount) / gamepackSize);
                    }
                });
            }
            final OutputStream fileStream = new FileOutputStream(destinationFile);
            try {
                fileStream.write(gamepackBytes);
                fileStream.flush();
            } finally {
                fileStream.close();
            }
        } finally {
            gamepackStream.close();
        }
        return null;
    }

    @Override
    protected final void process(final List<Integer> chunks) {
        for (final Integer percentage : chunks) {
            if (percentage == Integer.MIN_VALUE) {
                progressBar.setIndeterminate(true);
                return;
            }
            progressBar.setValue(percentage);
        }
    }
}
