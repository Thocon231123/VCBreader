package com.vcbreader;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText etOwnerName;
    private TextView tvStatus;
    private Button btnSave, btnPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etOwnerName = findViewById(R.id.et_owner_name);
        tvStatus = findViewById(R.id.tv_status);
        btnSave = findViewById(R.id.btn_save);
        btnPermission = findViewById(R.id.btn_permission);

        // Load tên đã lưu
        SharedPreferences prefs = getSharedPreferences("vcb_prefs", MODE_PRIVATE);
        etOwnerName.setText(prefs.getString("owner_name", "ông chủ"));

        btnSave.setOnClickListener(v -> {
            String name = etOwnerName.getText().toString().trim();
            if (name.isEmpty()) name = "ông chủ";
            prefs.edit().putString("owner_name", name).apply();
            Toast.makeText(this, "Đã lưu: " + name, Toast.LENGTH_SHORT).show();
        });

        btnPermission.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        if (isAccessibilityEnabled()) {
            tvStatus.setText("✅ Đang hoạt động\nSẽ đọc to khi có thông báo VCB");
            tvStatus.setTextColor(0xFF4CAF50);
            btnPermission.setText("Mở cài đặt quyền");
        } else {
            tvStatus.setText("❌ Chưa cấp quyền\nNhấn nút bên dưới để cấp quyền");
            tvStatus.setTextColor(0xFFE53935);
            btnPermission.setText("Cấp quyền ngay");
        }
    }

    private boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> services = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : services) {
            if (info.getId().contains(getPackageName())) return true;
        }
        return false;
    }
}
