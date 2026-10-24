package com.example.iot3.ui;

import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AlertDialog;
import android.text.InputFilter;
import com.example.iot3.MainActivity;
import com.example.iot3.R;
import com.example.iot3.data.EnvironmentData;
import com.example.iot3.data.MonitorViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

/** Local presentation settings only. No device commands or credentials. */
public final class DeviceSettings {
    private final AppCompatActivity activity;
    private final View root;
    private final MonitorViewModel model;

    public DeviceSettings(AppCompatActivity activity, View root, MonitorViewModel model,
                          Runnable onScheduleChanged) {
        this.activity = activity;
        this.root = root;
        this.model = model;
        root.findViewById(R.id.rename).setOnClickListener(view -> rename());
        root.findViewById(R.id.theme).setOnClickListener(view -> chooseTheme());
        root.findViewById(R.id.interval).setOnClickListener(view -> {
            int[] seconds = {5, 10, 30};
            new MaterialAlertDialogBuilder(activity).setTitle("云端数据刷新频率")
                    .setSingleChoiceItems(new String[]{"每 5 秒", "每 10 秒", "每 30 秒"},
                            model.refreshSeconds == 5 ? 0 : model.refreshSeconds == 10 ? 1 : 2,
                            (dialog, which) -> {
                                model.refreshSeconds = seconds[which];
                                render(model.current());
                                onScheduleChanged.run();
                                dialog.dismiss();
                            }).setNegativeButton("取消", null).show();
        });
        MaterialSwitch demo = root.findViewById(R.id.demo_switch);
        demo.setChecked(model.autoRefresh);
        demo.setOnCheckedChangeListener((button, checked) -> {
            model.autoRefresh = checked;
            onScheduleChanged.run();
        });
    }

    public void render(EnvironmentData data) {
        MainActivity.setText(root, R.id.device_name, data.device.name);
        MainActivity.setText(root, R.id.device_id, data.device.id);
        MainActivity.setText(root, R.id.device_status,
                model.connectionStatus() + " · 设备在线状态未提供");
        MainActivity.setText(root, R.id.interval, "云端刷新 · 每 " + model.refreshSeconds + " 秒");
        MainActivity.setText(root, R.id.theme, "显示主题 · "
                + (model.themeMode == AppCompatDelegate.MODE_NIGHT_YES ? "深色"
                : model.themeMode == AppCompatDelegate.MODE_NIGHT_NO ? "浅色" : "跟随系统"));
    }

    private void rename() {
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setText(model.current().device.name);
        input.setSelectAllOnFocus(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(24)});
        input.setContentDescription("设备显示名称，最多 24 字");
        int padding = Math.round(24 * activity.getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setTitle("设备显示名称")
                .setMessage("仅修改 APP 内的名称")
                .setView(input).setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        input.setError("请输入设备名称");
                        return;
                    }
                    EnvironmentData.Device old = model.current().device;
                    model.updateDevice(new EnvironmentData.Device(name, old.id, old.online));
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void chooseTheme() {
        int[] modes = {AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES};
        new MaterialAlertDialogBuilder(activity).setTitle("显示主题")
                .setSingleChoiceItems(new String[]{"跟随系统", "浅色", "深色"},
                        model.themeMode == modes[2] ? 2 : model.themeMode == modes[1] ? 1 : 0,
                        (dialog, which) -> {
                            model.themeMode = modes[which];
                            dialog.dismiss();
                            applyTheme(activity, model.themeMode);
                        }).setNegativeButton("取消", null).show();
    }

    public static void applyTheme(AppCompatActivity activity, int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
