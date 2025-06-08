package com.example.screenrotationhelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Sadece telefonun boot tamamlandı yayınını aldığından emin ol
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ServiceStateRepository repository = new ServiceStateRepository(context);

            // Kullanıcı daha önce servisi "aktif" olarak bıraktıysa...
            if (repository.isServiceEnabled()) {
                Log.d(TAG, "Telefon yeniden başlatıldı. Kayıtlı durum aktif, servis başlatılıyor.");
                Intent serviceIntent = new Intent(context, OverlayService.class);

                // Android 8 (Oreo) ve sonrası için startForegroundService kullanılmalı
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } else {
                Log.d(TAG, "Telefon yeniden başlatıldı. Kayıtlı durum pasif, servis başlatılmadı.");
            }
        }
    }
}