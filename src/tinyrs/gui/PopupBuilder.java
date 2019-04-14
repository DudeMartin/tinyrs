package tinyrs.gui;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class PopupBuilder {

    private Component parent;
    private String title;
    private String message;
    private int messageType;
    private Icon icon;

    public PopupBuilder withParent(final Component parent) {
        this.parent = nonNull(parent);
        return this;
    }

    public PopupBuilder withTitle(final String title) {
        this.title = nonNull(title);
        return this;
    }

    public PopupBuilder withMessage(final String message) {
        this.message = nonNull(message);
        return this;
    }

    public PopupBuilder withMessageType(final int messageType) {
        this.messageType = messageType;
        return this;
    }

    public PopupBuilder withIcon(final Icon icon) {
        this.icon = nonNull(icon);
        return this;
    }

    public void showMessage() {
        runTask(new Runnable() {

            @Override
            public void run() {
                JOptionPane.showMessageDialog(parent, message, title, messageType, icon);
            }
        });
    }

    public String showTextInput() {
        return runTask(new Callable<String>() {

            @Override
            public String call() {
                return (String) JOptionPane.showInputDialog(parent, message, title, messageType, icon, null, null);
            }
        });
    }

    public int showYesNoInput() {
        return runTask(new Callable<Integer>() {

            @Override
            public Integer call() {
                return JOptionPane.showOptionDialog(
                        parent,
                        message,
                        title,
                        JOptionPane.YES_NO_OPTION,
                        messageType,
                        icon,
                        null,
                        null);
            }
        });
    }

    private static <T> T runTask(final Callable<T> task) {
        final RunnableFuture<T> runnableFutureTask = new FutureTask<T>(task);
        runTask(runnableFutureTask);
        try {
            return runnableFutureTask.get();
        } catch (final ExecutionException impossible) {
            throw new Error(impossible);
        } catch (final InterruptedException impossible) {
            throw new Error(impossible);
        }
    }

    private static void runTask(final Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            } catch (final InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static <T> T nonNull(final T object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return object;
    }
}
