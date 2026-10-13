package ruby.systems.gui;

import ruby.RubyClient;

/** One clipboard writer. Parallel writes into SDL were locking the window. */
public final class SystemClipboard {
    private static final Object LOCK = new Object();
    private static String pending;
    private static boolean running;
    private static String lastText = "";
    private static long lastWrite;

    private SystemClipboard() {}

    public static void set(String text) {
        if (text == null) return;
        synchronized (LOCK) {
            pending = text;
            if (running) return;
            running = true;
        }
        Thread worker = new Thread(SystemClipboard::drain, "ruby-clipboard");
        worker.setDaemon(true);
        worker.start();
    }

    private static void drain() {
        while (true) {
            String text;
            synchronized (LOCK) {
                text = pending;
                pending = null;
                if (text == null) {
                    running = false;
                    return;
                }
            }
            long now = System.nanoTime();
            if (text.equals(lastText) && now - lastWrite < 250_000_000L) continue;
            try {
                RubyClient.client.keyboardHandler.setClipboard(text);
                lastText = text;
                lastWrite = now;
            } catch (Throwable ignored) {}
        }
    }
}
