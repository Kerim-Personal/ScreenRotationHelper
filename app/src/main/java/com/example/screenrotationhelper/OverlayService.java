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

        // Gerekli izin yoksa servisi sonlandır
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay izni gerekli", Toast.LENGTH_SHORT).show();
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

        // Sol alt köşe
        params.gravity = Gravity.START | Gravity.BOTTOM;
        params.x = 0;
        params.y = 0;

        // Buton görünümünü bağla
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.overlay_button, null);

        // Butonu tanımla ve tıklama olayını ayarla
        ImageButton rotateButton = floatingView.findViewById(R.id.rotateButton);
        rotateButton.setOnClickListener(v -> {
            try {
                Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                Toast.makeText(this, "Ekran döndürme kapatıldı", Toast.LENGTH_SHORT).show();
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
