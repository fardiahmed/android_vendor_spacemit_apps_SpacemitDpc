/*
 * Copyright (C) 2026 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.spacemit.dpc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Nudges the enroll service as early as possible on boot (direct-boot aware, so
 * it can fire at LOCKED_BOOT_COMPLETED before the setup activity runs).
 */
public final class AutoEnrollReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, EnrollService.class));
    }
}
