/*
 * Copyright (C) 2026 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.spacemit.dpc;

import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;

/** The device-admin component that becomes device owner for the appliance. */
public final class SpacemitDeviceAdminReceiver extends DeviceAdminReceiver {

    /** The admin ComponentName used by provisioning and policy calls. */
    public static ComponentName component(Context context) {
        return new ComponentName(context, SpacemitDeviceAdminReceiver.class);
    }
}
