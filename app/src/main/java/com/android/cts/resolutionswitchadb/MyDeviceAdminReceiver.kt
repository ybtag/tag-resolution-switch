package com.android.cts.resolutionswitchadb

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class MyDeviceAdminReceiver : DeviceAdminReceiver() {
// Enable device Admin api
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }
}