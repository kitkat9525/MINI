package com.kitkat.android.mini;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

public class PermissionManager {
    private static final int REQUEST_PERMISSION = 0;
    private static Callback callback;

    private static final String[] permissionArr = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE
    };

    @TargetApi(Build.VERSION_CODES.M)
    public static void checkPermission(Callback object) {
        callback = object;
        Activity activity = callback.getActivity();

        boolean flag = true;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permissipn : permissionArr) {
                if (activity.checkSelfPermission(permissipn) != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(permissionArr, REQUEST_PERMISSION);
                    flag = false;
                    break;
                }
            }

            if(flag)
                callback.BLESupportCheck();
        } else
            callback.BLESupportCheck();
    }

    public static void onCheckResult(int requestCode, int[] grantResults) {
        if(requestCode == REQUEST_PERMISSION) {
            for(int grantResult : grantResults) {
                if(grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(callback.getActivity(), "User PermissionManager Required!", Toast.LENGTH_SHORT).show();
                    break;
                } else {
                    callback.BLESupportCheck();
                }
            }
        }
    }

    interface Callback {
        Activity getActivity();
        void init();
        void BLESupportCheck();
    }
}
