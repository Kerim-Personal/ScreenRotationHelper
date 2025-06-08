package com.example.screenrotationhelper;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private static final int WRITE_SETTINGS_REQ_CODE = 1235;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startServiceButton = findViewById(R.id.startServiceButton);
        startServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // İzin kontrollerini başlat
                checkOverlayPermission();
            }
        });
    }

    // 1. Adım: Overlay iznini kontrol et
    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            } else {
                // Overlay izni varsa, sistem ayarları iznini kontrol et
                checkWriteSettingsPermission();
            }
        } else {
            // Eski sürümlerde doğrudan sistem ayarları iznini kontrol et
            checkWriteSettingsPermission();
        }
    }

    // 2. Adım: Sistem ayarlarını değiştirme iznini kontrol et
    private void checkWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, WRITE_SETTINGS_REQ_CODE);
            } else {
                // Her iki izin de varsa servisi başlat
                startOverlayService();
            }
        } else {
            // Eski sürümlerde doğrudan servisi başlat
            startOverlayService();
        }
    }

    // 3. Adım: Gerekli tüm izinler alındıysa servisi başlat
    private void startOverlayService() {
        startService(new Intent(this, OverlayService.class));
        Toast.makeText(this, "Servis başlatıldı.", Toast.LENGTH_SHORT).show();
        finish(); // Aktiviteyi kapat
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // Overlay izni verildi, şimdi diğer izni kontrol et
                    checkWriteSettingsPermission();
                } else {
                    Toast.makeText(this, "Diğer uygulamaların üzerinde gösterme izni gerekli.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == WRITE_SETTINGS_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(this)) {
                    // Sistem ayarları izni de verildi, servisi başlat
                    startOverlayService();
                } else {
                    Toast.makeText(this, "Sistem ayarlarını değiştirme izni gerekli.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}