package com.example.screenrotationhelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
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
    private int lastKnownDeviceOrientation = -1; // -1 = bilinmiyor

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startAsForegroundService(); // Servisin Android tarafından kapatılmasını engelle

        setupViews();
        setupLayoutParams();
        setupClickListener();
        setupOrientationListener();
        Log.d(TAG, "Servis başarıyla kuruldu ve oryantasyon dinliyor.");
    }

    private void startAsForegroundService() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Ekran Döndürme Servisi",
                    NotificationManager.IMPORTANCE_LOW); // Düşük öncelik, ses çıkarmaz
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Ekran Döndürme Yardımcısı")
                .setContentText("Servis arka planda çalışıyor.")
                .setSmallIcon(R.drawable.x) // Bildirim ikonu
                .setPriority(NotificationCompat.PRIORITY_LOW)
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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.END | Gravity.BOTTOM;
        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }

    private void setupClickListener() {
        ImageButton rotateButton = floatingView.findViewById(R.id.rotateButton);
        rotateButton.setOnClickListener(v -> {
            if (!isButtonShowing) return;

            try {
                // Tıklandığı andaki fiziksel yön neyse, ekranı o yöne kilitle
                if (lastKnownDeviceOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }
                windowManager.updateViewLayout(floatingView, params);
                hideButton();
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

                // Otomatik döndürme açıksa hiçbir şey yapma
                try {
                    if (Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 1) {
                        hideButton(); return;
                    }
                } catch (Settings.SettingNotFoundException e) { /* ignore */ }

                // 1. Cihazın FİZİKSEL yönünü daha kararlı bir mantıkla belirle
                int currentOrientation;
                if ((orientation > 45 && orientation < 135) || (orientation > 225 && orientation < 315)) {
                    currentOrientation = Configuration.ORIENTATION_LANDSCAPE;
                } else {
                    currentOrientation = Configuration.ORIENTATION_PORTRAIT;
                }

                // Sadece yön değiştiyse durumu güncelle (titremeyi engeller)
                if (lastKnownDeviceOrientation != currentOrientation) {
                    lastKnownDeviceOrientation = currentOrientation;
                }

                // 2. Ekranın KİLİTLİ yönünü belirle
                int lockedOrientation = (params.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        ? Configuration.ORIENTATION_LANDSCAPE
                        : Configuration.ORIENTATION_PORTRAIT;

                // 3. FİZİKSEL yön ile KİLİTLİ yön farklıysa butonu göster
                if (lastKnownDeviceOrientation != lockedOrientation) {
                    showButton();
                } else {
                    hideButton();
                }
            }
        };

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        } else {
            stopSelf(); // Sensör yoksa servis çalışamaz.
        }
    }

    private void showButton() {
        if (!isButtonShowing) {
            isButtonShowing = true; // Ekleme işleminden önce durumu değiştir
            try {
                windowManager.addView(floatingView, params);
            } catch (Exception e) {
                isButtonShowing = false; // Başarısız olursa durumu geri al
                Log.e(TAG, "Buton eklenirken hata.", e);
            }
        }
    }

    private void hideButton() {
        if (isButtonShowing) {
            isButtonShowing = false; // Kaldırma işleminden önce durumu değiştir
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                Log.e(TAG, "Buton kaldırılırken hata.", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Servis yok ediliyor.");
        hideButton();
        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }
    }
}