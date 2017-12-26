package ru.titub.morze;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * Created by CheRIP on 29.10.2015.
 */
//Класс для доступа к настройкам программы

public class MorzeSettings{
    private String TAG = "MorzeLog";

    private String MUTE = "mute";
    boolean mute=false;              //значение по умолчанию для мута рингтона

    private String NDIGITS = "nDigits";
    int nDigits=4;                //значение по умолчанию для количества цифр для проигрывания

    private String TURNONOFF = "turnOnOff";
    boolean turnOnOff = true;

    private String AUTORUN = "autoRun";
    boolean autoRun = true;

    private String LOOPCOUNT = "loop_count";
    int loop_count = 3;

    SharedPreferences sPref;
    Context ctx;

    public MorzeSettings(Context ctx){
        this.ctx = ctx;
        sPref = ctx.getSharedPreferences("morzeset", ctx.MODE_PRIVATE);
        //if(!sPref.contains())
     }

    public int load(){

        mute = sPref.getBoolean(MUTE,false);
        nDigits = sPref.getInt(NDIGITS, 4);
        turnOnOff = sPref.getBoolean(TURNONOFF, true); //получение состояния службы, работает или нет
        autoRun = sPref.getBoolean(AUTORUN,true);
        loop_count = sPref.getInt(LOOPCOUNT,3);
        Log.d(TAG,"nDigits = " + nDigits);
        Log.d(TAG,"turnOnOff = " + turnOnOff);
        return 1;
    }

    public int save(){
        Editor ed = sPref.edit();
        ed.putBoolean(MUTE,mute);
        ed.putInt(NDIGITS, nDigits);
        ed.putBoolean(TURNONOFF, turnOnOff);
        ed.putBoolean(AUTORUN, autoRun);
        ed.putInt(LOOPCOUNT,loop_count);
        ed.commit();
        return 1;
    }

}
