package ru.titub.morze;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.util.Log;
import android.widget.TextView;

/**
 * Call detect service. 
 * This service is needed, because MainActivity can lost it's focus,
 * and calls will not be detected.
 * 
 * @author Moskvichev Andrey V.
 *
 */
public class CallDetectService extends Service {
	private CallHelper callHelper;
	private MorzeSettings morzeSettings;
	private String TAG = "MorzeLog";
	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();
	private boolean isStart = false; //запущена ли служба
	//private boolean

    public CallDetectService() {
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Осуществлен onStartCommand");
		if(!isStart) {                                                    //Если служба не была запущена
			morzeSettings = new MorzeSettings(this);
			morzeSettings.load();
			Log.d(TAG, "intent.getBooleanExtra(isAutorun,false) = " + intent.getBooleanExtra("isAutorun", false));//загрузка настроек
			Log.d(TAG, "morzeSettings.autoRun = " + morzeSettings.autoRun);
			if(intent.getBooleanExtra("isAutorun",false) && !morzeSettings.autoRun)this.stopSelf();  //Если служба запущена из автозапуска, но в настройках автозапуск отключен
			callHelper = new CallHelper(this, getResources(), getPackageName(), morzeSettings);                     //Создание слушателя
			if (morzeSettings.turnOnOff) {                                                           //Если слушатель должен работать
				callHelper.start();
		/*int numOfDigits = intent.getIntExtra("Digits",7);
		//Log.d(TAG, "Полученное значение intent.getIntExtra = " + numOfDigits);
		callHelper.setNumOfDigits(numOfDigits);*/
				//Integer ir_numOfDigits = Integer.valueOf(morzeSettings.nDigits);   //получение количества проигрываемых цифр из morzeSettings
				//int i_numOfDigits = ir_numOfDigits.intValue();                     //преобразование из строкового в целое
			}
		}
		int res = super.onStartCommand(intent, flags, startId);
        isStart = true;  //установка флага запуска службы для обработки повторных вызовов onStartCommand
		//callHelper.textViewDetectState = textViewDetectState;
		return res;
	}
	
    @Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Осуществлен onDestroy ");
		morzeSettings.save();                      //сохранение настроек программы
		callHelper.stop();
	}

	/**
	 * Class used for the client Binder.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		CallDetectService getService() {
			// Return this instance of LocalService so clients can call public methods
			return CallDetectService.this;
		}
	}

	@Override
    public IBinder onBind(Intent intent) {
		Log.d(TAG, "Осуществлен onBind");
		/*callHelper = new CallHelper(this, getResources(), getPackageName());
		callHelper.start();*/
		// not supporting binding
    	return mBinder;
    }

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "Осуществлен метод onUnbind");
		return super.onUnbind(intent);
	}

	//Функция получения настроек службы
	public MorzeSettings getSettings(){
		Log.d(TAG, "Осуществлен вызов метода службы. getSettings");
		return morzeSettings;
	}

	//функция включения и выключения слушателя
	public void turnCallHelper(){
		if(morzeSettings.turnOnOff){
			callHelper.stop();               //отключение слушателя
			Log.d(TAG, "Осуществлен вызов метода службы turnCallHelper. callHelper.stop()");
			morzeSettings.turnOnOff = false; //установка флага нерабочего положения слушателя звонков
		}
		else {
			callHelper.start();               //включение слушателя
			Log.d(TAG, "Осуществлен вызов метода службы turnCallHelper. callHelper.start()");
			morzeSettings.turnOnOff = true;  //установка флага рабочего положения слушателя звонков
		}
	}
}
