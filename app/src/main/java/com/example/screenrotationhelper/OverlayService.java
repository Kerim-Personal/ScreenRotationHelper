package com.example.screenrotationhelper;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private View floatingView;

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Bound service kullanmıyoruz
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        // Sağ alt köşe olarak güncellendi
        params.gravity = Gravity.END | Gravity.BOTTOM;
        params.x = 0;
        params.y = 0;

        // Buton görünümünü bağla
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.overlay_button, null);

        // Butonu tanımla ve tıklama olayını ayarla
        ImageButton rotateButton = floatingView.findViewById(R.id.rotateButton);
        rotateButton.setOnClickListener(v -> {
            try {
                // Ekran döndürme ayarını değiştir (0: kapalı, 1: açık)
                int currentRotation = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
                int newRotation = (currentRotation == 0) ? 1 : 0;
                Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, newRotation);

                String message = (newRotation == 0) ? "Ekran döndürme kapatıldı" : "Ekran döndürme açıldı";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Butonu ekrana yerleştir
        windowManager.addView(floatingView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}