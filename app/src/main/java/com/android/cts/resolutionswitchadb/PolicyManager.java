package com.android.cts.resolutionswitchadb;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;

public class PolicyManager {
    public static final int PM_ACTIVATION_REQUEST_CODE = 101;
    private Context _mContext;
    private DevicePolicyManager _mDPM;
    private ComponentName _adminComponent;

    public PolicyManager(Context context) {
        this._mContext = context;
        _mDPM = (DevicePolicyManager) _mContext
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        _adminComponent = new ComponentName(_mContext.getPackageName(),
                _mContext.getPackageName() + ".MyDeviceAdminReceiver");
    }
}