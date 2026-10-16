/*
 * Copyright (C) 2026 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.spacemit.dpc;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.app.admin.FullyManagedDeviceProvisioningParams;
import android.app.admin.ProvisioningException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Slog;

/**
 * First-boot auto-enrollment for the appliance model: makes this app the device
 * owner without QR, NFC or adb, then marks setup complete and applies the baked
 * policy. Requires a factory-fresh device (no device owner, setup not complete,
 * no accounts) and runs best when no SetupWizard has marked the device
 * provisioned first — see the README.
 */
public final class EnrollService extends Service {

    private static final String TAG = "SpacemitDpc";
    private static final String OWNER_NAME = "SpacemiT";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Provisioning can be heavy; keep it off the main thread.
        new Thread(() -> {
            enroll();
            stopSelf(startId);
        }, "SpacemitDpcEnroll").start();
        return START_NOT_STICKY;
    }

    private void enroll() {
        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
        if (dpm == null || dpm.isDeviceOwnerApp(getPackageName())) {
            return; // already enrolled, or no DPM
        }
        int pre = dpm.checkProvisioningPrecondition(
                DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE, getPackageName());
        if (pre != DevicePolicyManager.STATUS_OK) {
            Slog.w(TAG, "Not enrolling, precondition=" + pre
                    + " (4=setup already complete, 1=has owner, 6=accounts exist)");
            return;
        }

        ComponentName admin = SpacemitDeviceAdminReceiver.component(this);
        FullyManagedDeviceProvisioningParams params =
                new FullyManagedDeviceProvisioningParams.Builder(admin, OWNER_NAME)
                        .setLeaveAllSystemAppsEnabled(true)
                        .build();
        try {
            dpm.provisionFullyManagedDevice(params);
        } catch (ProvisioningException e) {
            Slog.e(TAG, "provisionFullyManagedDevice failed", e);
            return;
        }
        Slog.i(TAG, "Device owner set to " + getPackageName());

        // Leave provisioning so the device boots to the launcher.
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);

        applyBakedPolicy(dpm, admin);
    }

    /** Minimal appliance policy; extend with kiosk/lock-task or SpacemitPolicy keys. */
    private void applyBakedPolicy(DevicePolicyManager dpm, ComponentName admin) {
        dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER);
        dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET);
    }
}
