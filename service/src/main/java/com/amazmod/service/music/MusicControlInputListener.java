package com.amazmod.service.music;

        import android.content.Context;
        import android.os.PowerManager;

        import com.amazmod.service.events.HardwareButtonEvent;

        import org.tinylog.Logger;

        import java.io.File;
        import java.io.FileInputStream;
        import java.io.FileNotFoundException;
        import java.nio.ByteBuffer;
        import java.nio.ByteOrder;
        import java.util.concurrent.Callable;
        import java.util.concurrent.ExecutorService;
        import java.util.concurrent.Executors;
        import java.util.concurrent.FutureTask;

        import org.greenrobot.eventbus.EventBus;

        import static android.content.Context.POWER_SERVICE;

public class MusicControlInputListener {

    private final int TYPE_KEYBOARD = 1;

    public static final int KEY_DOWN = 64;
    public static final int KEY_CENTER = 116;
    public static final int KEY_UP = 63;

    private final int KEY_EVENT_UP = 0;
    private final int KEY_EVENT_PRESS = 1;

    private final int TRIGGER = 500;
    private final int LONG_TRIGGER = TRIGGER * 4;
    private final int LONG_TRIGGER_MAX = TRIGGER * 10;

    private boolean listening;

    ExecutorService executor;

    public void start(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "AmazMod:music-control");
        wakeLock.acquire(10*60*1000L /*10 minutes*/);

        FutureTask<Void> futureTask = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                long lastKeyDownKeyDown = 0;
                int lastKeyCenterKeyUp = 0;
                int lastKeyUpKeyUp = 0;

                File file = new File("/dev/input/event2");

                try {
                    FileInputStream fileInputStream = new FileInputStream(file);

                    while (true) {
                        byte[] buffer = new byte[24];
                        fileInputStream.read(buffer, 0, 24);

                        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(24);
                        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                        byteBuffer.put(buffer);

                        int timeS = byteBuffer.getInt(0);
                        int timeMS = byteBuffer.getInt(4);
                        short type = byteBuffer.getShort(8);
                        short code = byteBuffer.getShort(10);
                        short value = byteBuffer.getShort(12);

                        //Logger.debug("timeS: " + timeS + ", timeMS: " + timeMS + ", type: " + type + ", code: " + code + ", value: " + value);

                        if (type != TYPE_KEYBOARD) {
                            continue;
                        }

                        long now = System.currentTimeMillis();

                        switch (code) {
                            case KEY_DOWN: {
                                if (value == KEY_EVENT_UP) {
                                    long delta = now - lastKeyDownKeyDown;
                                    if ((delta > TRIGGER) && (delta < LONG_TRIGGER)) {
                                        Logger.debug("long key down detected");
                                        EventBus.getDefault().post(new HardwareButtonEvent(KEY_DOWN, true));
                                    } else {
                                        if (delta < TRIGGER) {
                                            Logger.debug("key down detected");
                                            EventBus.getDefault().post(new HardwareButtonEvent(KEY_DOWN, false));
                                        }
                                    }
                                } else if (value == KEY_EVENT_PRESS) {
                                    lastKeyDownKeyDown = now;
                                }
                                break;
                            }
                            case KEY_CENTER: {
                                // not handled at the moment
                                break;
                            }
                            case KEY_UP: {
                                // not handled at the moment
                                break;
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Logger.debug("event file not found");
                }

                return null;
            }
        });

        executor = Executors.newFixedThreadPool(1);
        executor.execute(futureTask);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
            listening = false;
        }
    }

    public boolean isListening() {
        return listening;
    }
}
