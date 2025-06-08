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
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

public class MainActivity extends AppCompatActivity {

    private Button serviceButton;
    private ServiceStateRepository serviceStateRepository; // Hafıza yöneticimizi ekledik

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                checkAllPermissions();
            });

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                checkAllPermissions();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceStateRepository = new ServiceStateRepository(this); // Yöneticiyi oluşturduk
        serviceButton = findViewById(R.id.startServiceButton);
        serviceButton.setOnClickListener(v -> toggleService());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtonState();
        checkAllPermissions();
    }

    private void checkAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Diğer uygulamalar üzerinde gösterme izni gerekli.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            settingsLauncher.launch(intent);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            Toast.makeText(this, "Sistem ayarlarını değiştirme izni gerekli.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
            settingsLauncher.launch(intent);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
                return;
            }
        }
        updateButtonState();
    }

    private void toggleService() {
        if (isServiceRunning(OverlayService.class)) {
            // Servisi durdur ve durumunu "pasif" olarak kaydet
            stopService(new Intent(this, OverlayService.class));
            serviceStateRepository.setServiceState(false);
            Toast.makeText(this, "Servis durduruldu.", Toast.LENGTH_SHORT).show();
        } else {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this) && Settings.System.canWrite(this))
                    || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                // Servisi başlat ve durumunu "aktif" olarak kaydet
                startService(new Intent(this, OverlayService.class));
                serviceStateRepository.setServiceState(true);
                Toast.makeText(this, "Servis başlatıldı.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Lütfen gerekli tüm izinleri verin.", Toast.LENGTH_LONG).show();
                checkAllPermissions();
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