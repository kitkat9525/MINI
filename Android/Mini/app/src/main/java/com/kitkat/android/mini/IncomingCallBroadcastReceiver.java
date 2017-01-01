package com.kitkat.android.mini;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

public class IncomingCallBroadcastReceiver extends BroadcastReceiver {
    private Callback callback;
    private static String mLastState;

    public IncomingCallBroadcastReceiver() {}
    public IncomingCallBroadcastReceiver(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TelephonyManager.EXTRA_STATE_IDLE        Call End or Ringing End..
        // TelephonyManager.EXTRA_STATE_RINGING     Ringing..
        // TelephonyManager.EXTRA_STATE_OFFHOOK     Calling..

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (state.equals(mLastState))
            return;
        else
            mLastState = state;

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            final String phone_number = PhoneNumberUtils.formatNumber(incomingNumber);
            String msg = "#".concat(phone_number);
            callback.receivedIncomingCall(msg);
        }

        else if(TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state) || TelephonyManager.EXTRA_STATE_IDLE.equals(state))
            callback.receivedIncomingCall("&");
    }

    interface Callback {
        void receivedIncomingCall(String msg);
    }
}
