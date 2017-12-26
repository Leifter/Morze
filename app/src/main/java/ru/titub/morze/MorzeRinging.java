package ru.titub.morze;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.media.MediaPlayer;
import android.content.res.Resources;
import android.util.Log;
import android.net.Uri;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import android.os.Build;
import android.util.TypedValue;

/**
 * Created by CheRIP on 11.10.2015.
 */
public class MorzeRinging {
    final int MAX_STREAMS = 3;
    final String TAG = "MorzeLog";

    private SoundPool sp;

    private MediaPlayer mp_error, mplayer;

    private boolean loaded=false;
    private final Context ctx;
    private Resources resources;
    private String packname;
    private ArrayList soundDescrs;

    public Thread soundThread;
    public boolean isRun=true; //статус потока проигрывания морзе кода

    private String[] translate,trans_from,trans_to; //массивы перевода
//@TargetApi(10)
  /*  public initSoundPool(){

    }*/

    public MorzeRinging(final Context ctx, Resources resources, String packname) {
        this.ctx = ctx;
        this.resources = resources;
        this.packname = packname;

        mp_error = MediaPlayer.create(ctx, R.raw.error);
        mplayer = new MediaPlayer();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {  //Для версий Android больше или равно 21
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            sp = new SoundPool.Builder()
                    .setAudioAttributes(attributes)
                    .build();
        }
        else{                                                        //Для версий Android меньше 21
            sp = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
        }

        sp.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;

            }
        });

        soundDescrs = initSoundDescrs(); //инициаллизация описателей сэмплов
        initTranslateArrays(); //Формирование массивов перевода символов текста

        Log.d(TAG, "Осуществлен MorzeRinging");

    }

    public int playWithDelay(final SoundPool sp,final int samplefile,final ArrayList sdescrs) {
        SampleDescr sd = findSoundDescrByRawId(samplefile, sdescrs);
        sp.play(sd.soundID, 1, 1, 0, 0, 1);
        try {
            //Log.d(TAG, "Вызывается Sleep с параметром sd.duration = " + sd.duration);
            Thread.sleep(sd.duration);
            } catch (InterruptedException ex) {
            Log.d(TAG, "Прерывание playWithDelay:sleep");
            }
        return 0;
    }

    public SampleDescr findSoundDescrByRawId(int samplefile, ArrayList sdescrs){
        SampleDescr sd,sd_found;
        sd_found = new SampleDescr();
        boolean found=false;
        for(int i=0;i<sdescrs.size();i++){
            sd=(SampleDescr)sdescrs.get(i);
            if(sd.samplefile==samplefile){
                if(!found){
                    sd_found = sd;
                    found = true;
                }
                else Log.d(TAG, "Повторное обнаружение дескриптора, удовлетворяющего указателю на файл = " + samplefile);
            }
        }
        if(!found)return null;
        return sd_found;
    }

    //метод инициаллизацтт описателей звуков
    ArrayList initSoundDescrs(){
        // sSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);

        MediaPlayer mp = new MediaPlayer();
        ArrayList soundList = new ArrayList();
        int[] samples = {R.raw.dash,R.raw.dot,R.raw.dash_silent,R.raw.dot_silent};
        for(int s:samples){
            int soundId = sp.load(ctx,s,1);
            int duration = getSampleDuration(mp, s);
            SampleDescr sd = new SampleDescr(soundId,s,duration);
            soundList.add(sd);
        }
        mp.release();
        return soundList;
    }

    private class SampleDescr{
        public int soundID;
        public int samplefile;
        public int duration;
        public SampleDescr(){}
        public SampleDescr(int soundID,int samplefile,int duration){
            this.soundID = soundID;
            this.samplefile = samplefile;
            this.duration=duration;
        }
    }

    public int getSampleDuration(MediaPlayer mp, int samplefile) {
        Uri uri;
        mp.reset();
        try {
            //Log.d(TAG, "Отработал trysetDataSource2. samplefile = " + samplefile);
            uri = Uri.parse("android.resource://"+ packname + "/" + samplefile); //это когда код для символов вычисляется
            /////uri = Uri.parse("android.resource://"+ packname +"/raw/" + samplefile); //это когда для символа идет готовый код
            mp.setDataSource(ctx, uri);
            mp.prepare();
            //mp.start();
            //afd.close();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Log.d(TAG, "mp.getDuration() = " + mp.getDuration());
        return mp.getDuration();
    }

    //Метод получения кода морзе для данного символа Unicode
    public int[] getMorzeCode(char ch){
        int res_id;
        int[] code;
        String res_name = "l_" + ch;
        res_id = resources.getIdentifier(res_name, "array", packname);
        if(res_id==0){
            Log.d(TAG,"Ошибка. не обнаружен res_id, res_name = " + res_name + " заменено на ресурс нуля");
            res_id = resources.getIdentifier("l_0", "array", packname);
        }
        //Log.d(TAG,"res_id = " + res_id);
        code = resources.getIntArray(res_id);
        //Log.d(TAG,"Количество элементов в code = " + code.length);
        return code;
    }

    public int playMorzeCode(int[] code){
        Log.d(TAG,"Функция playMorzeCodeRes. Количество элементов в code = " + code.length);
        for(int c : code){
            if(!isRun)return 0; //Если пришло сообщение об остановки проигрывания
            if(c == 0){         //проигрывать точку если 0
                playWithDelay(sp, R.raw.dot, soundDescrs);
            }
            else if(c == 1){    //проигрывать тире если 1
                playWithDelay(sp, R.raw.dash, soundDescrs);
            }
            else {              //если что-то не то проигрывать ошибку
                Log.d(TAG,"Функция playMorzeCode. Проигрывается ошибка");
                mp_error.start();
            }
            playWithDelay(sp, R.raw.dot_silent, soundDescrs);
         }
        return 1;
    }

    public int playNumber2(String telnumber, final int loop_count,
                           final boolean mute, final AudioManager manager, final int volume_start){
        telnumber = translateText(telnumber); //Функция приведения проигрываемого текста к стандартному виду
        final String tn = telnumber; //Финализация строки, чтобы нельзя ее было менять из текущего треда

        soundThread = new Thread() {
            @Override
            public void run() {
                char[] telnumber_char = tn.toCharArray(); //Разбиение строки на отдельные символы
                int[]code;
                for(int i=0;i < loop_count;i++)  //цикл по количеству проигрываний
                for(char tc:telnumber_char){
                    code = getMorzeCode(tc);
                    if(playMorzeCode(code)==0){   //Проигрывание кода морзе
                        unMute(mute, manager, volume_start); //вызвать метод снятия мьюта. Вообще кривовато как-то
                        return; //завершение run если getMorzeCode возвращает 0
                    }
                }
                unMute(mute, manager, volume_start); //вызвать метод снятия мьюта. Вообще кривовато как-то
            }

            public void unMute(boolean mute, AudioManager manager, int volume_start){
                //Раз мутить звонок после проигрывания
                if (mute) {
                    Log.d(TAG, "playnumber2::unMute - Unmute ring");
                    Log.d(TAG, "volume_start = " + volume_start);
                    manager.setStreamVolume(AudioManager.STREAM_RING, volume_start, 0);
                }
            }
        };
        soundThread.start();

        return 0;
    }

    void initTranslateArrays(){
        translate = resources.getStringArray(R.array.translate);  //Получить
        trans_from = new String[translate.length/2];
        trans_to = new String[translate.length/2];

        //Напоминание. добавить проверку четности массива translate
        Log.d(TAG, "translate.length/2 = " + translate.length/2);
        //Цикл разделения translate на trans_from и trans_to
        for (int i=0,j=0;i<translate.length;i+=2,j++) {
            trans_from[j]=translate[i];
            trans_to[j]=translate[i+1];
        }
    }
    //Функция перевода символов к одному виду. Кирилические к латыни, на перспективу иероглифы к латинским эквивалентам
    public String translateText(String text){
        text = text.toLowerCase();                        //Весь текст в нижний регистр
        for(int i=0;i<trans_from.length;i++){
            text = text.replace(trans_from[i],trans_to[i]);
        }
        Log.d(TAG, "text = " + text);
        return text;
    }
}
