package com.example.screenrotationhelper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button serviceButton;

    // Yeni ve modern izin isteme yöntemi
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                // İzin sonuçları geldikten sonra tekrar kontrol et
                checkAllPermissions();
            });

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Ayarlar ekranından döndükten sonra tekrar kontrol et
                checkAllPermissions();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceButton = findViewById(R.id.startServiceButton);
        serviceButton.setOnClickListener(v -> toggleService());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ekran her açıldığında durumu kontrol et
        updateButtonState();
        checkAllPermissions();
    }

    private void checkAllPermissions() {
        // Adım 1: Overlay iznini kontrol et
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Diğer uygulamalar üzerinde gösterme izni gerekli.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            settingsLauncher.launch(intent);
            return;
        }

        // Adım 2: Sistem ayarlarını yazma iznini kontrol et
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            Toast.makeText(this, "Sistem ayarlarını değiştirme izni gerekli.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
            settingsLauncher.launch(intent);
            return;
        }

        // Adım 3: Bildirim iznini kontrol et (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
                return;
            }
        }

        // Tüm izinler tamamsa butonun durumunu güncelle.
        updateButtonState();
    }

    private void toggleService() {
        if (isServiceRunning(OverlayService.class)) {
            stopService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "Servis durduruldu.", Toast.LENGTH_SHORT).show();
        } else {
            // Servisi başlatmadan önce tekrar izinleri kontrol et
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this) && Settings.System.canWrite(this))
                    || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                startService(new Intent(this, OverlayService.class));
                Toast.makeText(this, "Servis başlatıldı.", Toast.LENGTH_SHORT).show();
                finish(); // Servis başlayınca ana ekranı kapat
            } else {
                Toast.makeText(this, "Lütfen gerekli tüm izinleri verin.", Toast.LENGTH_LONG).show();
                checkAllPermissions(); // Kullanıcıyı tekrar izin ekranlarına yönlendir
            }
        }
        updateButtonState();
    }

    private void updateButtonState() {
        if (isServiceRunning(OverlayService.class)) {
            serviceButton.setText("Servisi Durdur");
        } else {
            serviceButton.setText("Servisi Başlat");
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}