package com.kitkat.android.mini;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private Callback callback;

    public MyBroadcastReceiver() {}
    public MyBroadcastReceiver(Callback callback){
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        Object[] message = (Object[]) bundle.get("pdus");
        SmsMessage[] smsMessage = new SmsMessage[message.length];

        for(int i=0; i<message.length; i++) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // over API 23
                String format = bundle.getString("format");
                smsMessage[i] = SmsMessage.createFromPdu((byte[]) message[i], format);
            } else {
                smsMessage[i] = SmsMessage.createFromPdu((byte[]) message[i]);
            }
        }

        String msg = smsMessage[0].getMessageBody().toString();
        // callback.received(msg);
        callback.receivedSMS(msg.substring(msg.indexOf("\n")+1, msg.length()));

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    interface Callback {
        void receivedSMS(String msg);
    }
}
