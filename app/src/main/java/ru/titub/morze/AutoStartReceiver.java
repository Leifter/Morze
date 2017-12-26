package ru.titub.morze;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoStartReceiver extends BroadcastReceiver {
    private String TAG = "MorzeLog";
    private final String BOOT_ACTION = "android.intent.action.BOOT_COMPLETED";
    Context mContext;
    public AutoStartReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String action = intent.getAction();
        if (action.equalsIgnoreCase(BOOT_ACTION)) {
            Log.d(TAG, "Осуществлен AutoStartReceiver:onReceive");

            Intent serviceIntent = new Intent(context, CallDetectService.class);
            serviceIntent.putExtra("isAutorun",true); //установка флага запуска службы по автозапуску
            context.startService(serviceIntent);
        }

        //throw new UnsupportedOperationException("Not yet implemented");
    }
}
