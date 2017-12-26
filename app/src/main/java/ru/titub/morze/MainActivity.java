package ru.titub.morze;

import android.media.MediaPlayer;
//import android.app.Activity;
//import android.R;
//import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.media.AudioManager;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.ComponentName;

public class MainActivity extends Activity {
//public class MainActivity extends AppCompatActivity {

    private TextView textViewDetectState;
    private Button buttonToggleDetect;
    private Button buttonExit;
    private CheckBox checkMute,checkAutostart;
    private EditText editDigits, editLoop;

    String TAG = "MorzeLog";

    MorzeRinging morzeRinging;
    AudioManager manager;

    CallDetectService mService;
    MorzeSettings morzeSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Получаем доступ к менеджеру звуков
 //       manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
//        morzeRinging = new MorzeRinging(this, getResources(), getPackageName()); //этот объект для тестов

        textViewDetectState = (TextView) findViewById(R.id.textViewDetectState); //получаем указатель на текстовый объек индикации состояния службы отслеживания входящих

        //Кнопка включения службы
        buttonToggleDetect = (Button) findViewById(R.id.buttonDetectToggle); //получаем указатель на кнопку включения службы отслеживания входящих
        buttonToggleDetect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {                       //обработчик нажатия кнопки
 //               setDetectEnabled(!detectEnabled);               //это для изменения текста кнопок. Нужно как-то объеденить с начальнои инициаллизацией
                mService.turnCallHelper();                      //поменять состояние слушателя звонков
                refreshSettings(mService);                      //Обновить интерфейс
                }
            }

            );
//R.array.translate
        buttonExit=(Button) findViewById(R.id.buttonExit);      //получаем указатель на кнопку выхода
        buttonExit.setOnClickListener(new OnClickListener() {
                @Override
            public void onClick(View v) {   //обработчик нажатия кнопки выхода
                //setDetectEnabled(false); //убрал остановку службы так как по выходу из программы она должна продолжать работать

                morzeSettings.save();                                   //Сохранение настроек при выходе из программы
                /*if(!morzeSettings.turnOnOff)closeService();             //закрытие службы для экономии памяти, если слушатель не включен
                *///доработать
                //Добавить закрытие службы и на случай если активити завершается?
                MainActivity.this.finish();
            }
        });

        checkMute =(CheckBox)findViewById(R.id.checkMute);  //получаем указатель на чекбокс мьюта рингтона
        checkMute.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {             //обработчик мьюта рингтона
                if (!checkMute.isChecked()) {
                    Log.d(TAG, "Mute Changed to not checked = ");
                    morzeSettings.mute=false;
                } else {
                    Log.d(TAG, "Mute Changed to checked = ");
                    morzeSettings.mute=true;
                }
            }
        });

        checkAutostart =(CheckBox)findViewById(R.id.checkAutostart);  //получаем указатель на чекбокс мьюта рингтона
        checkAutostart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {                    //обработчик чекбокса автозапуска
                if (!checkAutostart.isChecked()) {
                    Log.d(TAG, "Autorun changed not checked = ");
                    morzeSettings.autoRun = false;
                } else {
                    Log.d(TAG, "Autorun changed checked = ");
                    morzeSettings.autoRun = true;
                }
            }
        });

        final InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String blockCharacterSet = "~#^|$%*!@/()-'\":;,?{}=!$^';,?×÷<>{}€£¥₩%~`¤♡♥_|《》¡¿°•○●□■◇◆♧♣▲▼▶◀↑↓←→☆★▪:-);-):-D:-(:'(:O 1234567890";
                Log.d(TAG, "source = " + source);
                /*if (source != null && blockCharacterSet.contains(("" + source))) {
                    return "";
                }*/
                if(source=="0")return "1";
                return null;
            }
        };

        editDigits = (EditText)findViewById(R.id.editTextDigits);               //получаем указатель на количество проигрываемых цифр
        editDigits.setOnEditorActionListener(new TextView.OnEditorActionListener() {   //обработчик изменения значения текстового поля
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //v.getText();
                //Log.d(TAG,"actionId = "+actionId);
                CharSequence c_numOfDigits = v.getText();
                String s_numOfDigits = c_numOfDigits.toString();
                if (s_numOfDigits.equals("")) s_numOfDigits = "0"; //защита от пустого ввода
                Integer ir_numOfDigits = Integer.valueOf(s_numOfDigits);
                int i_numOfDigits = ir_numOfDigits.intValue();
                //здесь можно добавить проверку недопустимых значений
                if (i_numOfDigits == 0) {
                    i_numOfDigits = 1;
                    v.setText("1");
                }
                Log.d(TAG, "i_numOfDigits = " + i_numOfDigits);
                morzeSettings.nDigits = i_numOfDigits;    //Установка количества проигрываемых цифр
                return false;
            }
        });

        editLoop = (EditText)findViewById(R.id.editTextLoop);               //получаем указатель на количество повторов проигрывания
        editLoop.setOnEditorActionListener(new TextView.OnEditorActionListener() {   //обработчик изменения значения текстового поля
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                CharSequence c_numOfDigits = v.getText();
                String s_numOfDigits = c_numOfDigits.toString();
                if (s_numOfDigits.equals("")) s_numOfDigits = "1"; //защита от пустого ввода
                Integer ir_numOfDigits = Integer.valueOf(s_numOfDigits);
                int i_numOfDigits = ir_numOfDigits.intValue();
                //здесь можно добавить проверку недопустимых значений
                if (i_numOfDigits == 0) {
                    i_numOfDigits = 1;
                    v.setText("1");
                }
                Log.d(TAG, "loop_count = " + i_numOfDigits);
                morzeSettings.loop_count = i_numOfDigits;    //Установка количества проигрываемых цифр
                return false;
            }
        });
        editLoop.addTextChangedListener(new TextWatcher() {
            private boolean isInnerEdit=false;
            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "s.toString() = " + s.toString());
                if(s.toString().equals("0")){
                    Log.d(TAG, "s.toString() равна 0");
                    s.replace(0,1,"1");
                }
                //s.
                // Прописываем то, что надо выполнить после изменения текста
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d(TAG, "beforeTextChanged = " + s);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged = " + s);
            }
        });


        //editLoop.setFilters(new InputFilter[]{filter});

        //Запуск службы и формирование соединения Активити с ней
        Intent intent = new Intent(this, CallDetectService.class);   //формирование намерения для работы со службой
        startService(intent);                                        //Запуск службы
        boolean bindResult = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);  //Подключение службы к Activyty
        Log.d(TAG, "bindResult = " + bindResult);
         //checkMute.

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //Функция обновления интерфейса программы в соответсвии с настройками службы
    private void refreshSettings(CallDetectService mService){
        Integer ir_numOfDigits;
        if(morzeSettings.turnOnOff){                          //Отрисовка интерфейса в соответствии с настройками службы
            buttonToggleDetect.setText("Turn off");
            textViewDetectState.setText("Ringing");
        }
        else {
            buttonToggleDetect.setText("Turn on");
            textViewDetectState.setText("Not ringing");
        }
        checkAutostart.setChecked(morzeSettings.autoRun);             //установка чекбокса автозапуска
        checkMute.setChecked(morzeSettings.mute);                     //установка чекбокса mute

        ir_numOfDigits = Integer.valueOf(morzeSettings.nDigits);     //установка количества проигрываемых цифр
        editDigits.setText(ir_numOfDigits.toString());

        ir_numOfDigits = Integer.valueOf(morzeSettings.loop_count);  //установка количества проигрывания повторов
        editLoop.setText(ir_numOfDigits.toString());
        //editDigits.setText
        //morzeSettings.save();                             //сохранение настроек. Решить вопрос с сохранением настроек
    }

    private void closeService(){
        Intent intent = new Intent(this, CallDetectService.class);
        stopService(intent);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to CallDetectService, cast the IBinder and get CallDetectService instance
            CallDetectService.LocalBinder binder = (CallDetectService.LocalBinder) service;
            mService = binder.getService();
            morzeSettings = mService.getSettings();               //определения объекта настроек
            refreshSettings(mService);                            //Обновление интерфейса Activity в соответствии с настройками службы
            //mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
           // mBound = false;
        }
    };

/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
