package com.example.screenrotationhelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {
    private static final String TAG = "OverlayService";
    private static final String NOTIFICATION_CHANNEL_ID = "ScreenRotationChannel";
    private static final int NOTIFICATION_ID = 1;

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private OrientationEventListener orientationEventListener;

    private volatile boolean isButtonShowing = false;
    private int lastKnownPhysicalOrientation = Configuration.ORIENTATION_PORTRAIT;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startAsForegroundService();
        setupViews();
        setupLayoutParams();
        setupClickListener();
        setupOrientationListener();
        Log.d(TAG, "Servis kuruldu.");
    }

    /**
     * Servisin arkaplanda çalışmasını sağlar ve en az rahatsız edici
     * şekilde bir bildirim oluşturur.
     */
    private void startAsForegroundService() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        String channelId = NOTIFICATION_CHANNEL_ID;

        // Android 8 ve sonrası için Bildirim Kanalı oluşturuyoruz
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Önem seviyesini MINIMUM yaparak durum çubuğunda ikon görünmesini engelliyoruz.
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Ekran Döndürme Servisi",
                    NotificationManager.IMPORTANCE_MIN);

            channel.setDescription("Servisin arkaplanda çalışmasını sağlayan bildirim");
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Ekran Döndürme Yardımcısı")
                .setContentText("Servis aktif")
                .setSmallIcon(R.drawable.ic_rotate_elegant) // Şık ikonumuz
                .setOngoing(true)
                .setShowWhen(false) // Zaman damgasını gizler
                // Önceliği MINIMUM yaparak bildirimin en altta ve küçük görünmesini sağlıyoruz.
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void setupViews() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.overlay_button, null);
    }

    private void setupLayoutParams() {
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.END | Gravity.BOTTOM;
    }

    private void setupClickListener() {
        ImageButton rotateButton = floatingView.findViewById(R.id.rotateButton);
        rotateButton.setOnClickListener(v -> {
            if (!isButtonShowing) return;

            try {
                if (lastKnownPhysicalOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, Surface.ROTATION_90);
                } else {
                    Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, Surface.ROTATION_0);
                }
                hideButton();
                Log.d(TAG, "Ekran manuel olarak kilitlendi.");

            } catch (Exception e) {
                Log.e(TAG, "Rotasyon kilidi uygulanırken hata oluştu.", e);
            }
        });
    }

    private void setupOrientationListener() {
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;

                try {
                    if (Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 1) {
                        hideButton();
                        return;
                    }
                } catch (Settings.SettingNotFoundException e) {
                    // ignore
                }

                int currentPhysicalOrientation;
                if ((orientation >= 70 && orientation <= 110) || (orientation >= 250 && orientation <= 290)) {
                    currentPhysicalOrientation = Configuration.ORIENTATION_LANDSCAPE;
                } else if ((orientation >= 340 || orientation <= 20) || (orientation >= 160 && orientation <= 200)) {
                    currentPhysicalOrientation = Configuration.ORIENTATION_PORTRAIT;
                } else {
                    return;
                }

                lastKnownPhysicalOrientation = currentPhysicalOrientation;

                int currentLockedOrientation;
                try {
                    int rotation = Settings.System.getInt(getContentResolver(), Settings.System.USER_ROTATION);
                    if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                        currentLockedOrientation = Configuration.ORIENTATION_LANDSCAPE;
                    } else {
                        currentLockedOrientation = Configuration.ORIENTATION_PORTRAIT;
                    }
                } catch (Settings.SettingNotFoundException e) {
                    currentLockedOrientation = Configuration.ORIENTATION_PORTRAIT;
                }

                if (currentPhysicalOrientation != currentLockedOrientation) {
                    showButton();
                } else {
                    hideButton();
                }
            }
        };

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
            Log.d(TAG, "Oryantasyon dinleyicisi başlatıldı.");
        } else {
            Log.e(TAG, "Oryantasyon sensörü bulunamadı. Servis durduruluyor.");
            stopSelf();
        }
    }

    private void showButton() {
        if (!isButtonShowing) {
            isButtonShowing = true;
            try {
                if (floatingView.getWindowToken() == null) {
                    windowManager.addView(floatingView, params);
                    Log.d(TAG, "Buton gösterildi.");
                }
            } catch (Exception e) {
                isButtonShowing = false;
                Log.e(TAG, "Buton eklenirken hata.", e);
            }
        }
    }

    private void hideButton() {
        if (isButtonShowing) {
            isButtonShowing = false;
            try {
                if (floatingView.getWindowToken() != null) {
                    windowManager.removeView(floatingView);
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Servis yok ediliyor.");
        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }
        hideButton();
    }
}