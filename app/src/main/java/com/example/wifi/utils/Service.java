package com.example.wifi.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.wifi.enums.ServiceState;

public class Service {

    private static final String TAG = Service.class.getSimpleName();
    private static final String SERVICE_STATE_PREFS_NAME = "service_state";
    private static final String SERVICE_STATE_PREFS_KEY = "service_state";


    public void setState(Context context, ServiceState serviceState) {

        Log.d(TAG, "\n\nsetState: serviceState.name() " + serviceState.name());

        SharedPreferences.Editor editor = getEditor(context);

        editor.putString(SERVICE_STATE_PREFS_KEY, serviceState.name());

        editor.commit();
    }


    public ServiceState getState(Context context) {

        SharedPreferences sharedPreferences = getPreferences(context);

        String state = sharedPreferences.getString(SERVICE_STATE_PREFS_KEY, ServiceState.STOPPED.name());

        Log.d(TAG, "getState: state " + state);

        return ServiceState.valueOf(state);
    }


    /*
    * Get shared preference editor
    */
    private SharedPreferences.Editor getEditor(Context context) {
        return getPreferences(context).edit();
    }


    /*
    * Get shared preferences
    */
    private SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(SERVICE_STATE_PREFS_NAME, Context.MODE_PRIVATE);
    }
}
