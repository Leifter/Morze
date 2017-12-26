package ru.titub.morze;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.AudioManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.database.Cursor;



/**
 * Helper class to detect incoming and outgoing calls.
 * @author Moskvichev Andrey V.
 *
 */
public class CallHelper {
	//private TextView textViewDetectState;
	/**
	 * Listener to detect incoming calls. 
	 */
	private Context ctx;
	private TelephonyManager tm;
	private CallStateListener callStateListener;
	private MorzeRinging morzeRinging;
	private Resources resources;
	private String packname;

	private OutgoingReceiver outgoingReceiver;

	private String TAG = "MorzeLog";

	private int volume_start; //переменная для сохранения уровня громкости звонка

	//private int numOfDigits=4; //4-значение по умолчанию. Количество проигрываемых цифр
	private MorzeSettings morzeSettings;

	private AudioManager manager;

	public CallHelper(Context ctx, Resources resources, String packname, MorzeSettings morzeSettings) {
		this.ctx = ctx;
		this.resources = resources;
		this.packname = packname;
		this.morzeSettings = morzeSettings; //Передача настроек программы в коллхелпер

		// Получаем доступ к менеджеру звуков
		manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);

		morzeRinging = new MorzeRinging(ctx, resources, packname); //создание объекта проигрывания телефонного номера
		callStateListener = new CallStateListener(); //создание объекта для отслеживания входящих вызовов

		//Для исходящих вызовов
		//outgoingReceiver = new OutgoingReceiver();
	}

	/**
	 * Start calls detection.
	 */
	public void start() {
		tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);  //получение указателя на службу телефонии
		tm.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);      //настройка PhoneStateListener как слушателя входящих вызовов
		Log.d(TAG, "CallHelper.start");
		//для исходящих вызовов
		//IntentFilter intentFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL); //создание интента для перехвата исходящих
		//ctx.registerReceiver(outgoingReceiver, intentFilter);                          //Настройка outgoingReceiver на хз что, в общем для обработки исходящих
	}

	/**
	 * Stop calls detection.
	 */
	public void stop() {
		tm.listen(callStateListener, PhoneStateListener.LISTEN_NONE);  //изменеие статуса callStateListener на отмену прослушивания

		//Для исходящих вызовов
		//ctx.unregisterReceiver(outgoingReceiver);    //Деоигистрация outgoingReceiver
	}

	private class CallStateListener extends PhoneStateListener {

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
            String phonenumber;
			switch (state) {
			case TelephonyManager.CALL_STATE_RINGING:          //если звонит телефон
				Log.d(TAG, "Получено сообщение TelephonyManager.CALL_STATE_RINGING ");
				morzeRinging.isRun=true;  //установить разрешения для работы звукового треда
				// called when someone is ringing to this phone

				//Мутить звонок или нет
				if (morzeSettings.mute){
					Log.d(TAG, "Mute ring");
					volume_start = manager.getStreamVolume(AudioManager.STREAM_RING);
					manager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
				}

				String name; //имя абонента
                if(true){
					name = getNameByPhoneNumber(incomingNumber);
				}
				incomingNumber = incomingNumber.replace("+","");    //удаление "+" в начале номера
				//Всплывающее сообщение о пришедшем вызове
            	Toast.makeText(ctx,
						"Incoming: " + " тел." + incomingNumber,
						Toast.LENGTH_LONG).show();

				int l_incomingNumber = incomingNumber.length();
				if(morzeSettings.nDigits > l_incomingNumber)morzeSettings.nDigits=l_incomingNumber;
                phonenumber=incomingNumber.substring(incomingNumber.length()-morzeSettings.nDigits);
				Log.d(TAG, "phonenumber = " + phonenumber);
				if(true){
					name = morzeRinging.translateText(name);
					phonenumber = name+phonenumber;
				}
				morzeRinging.playNumber2(phonenumber, morzeSettings.loop_count, morzeSettings.mute, manager, volume_start); //Проигрывание номера азбукой морзе

				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				Log.d(TAG, "Получено сообщение TelephonyManager.CALL_STATE_OFFHOOK ");
				if (morzeRinging.soundThread != null) {
					if (morzeRinging.soundThread.isAlive()){
						morzeRinging.isRun=false;  //дать понять звуковому треду, чтобы он завершился
						Log.d(TAG, "Вызывается сообщение morzeRinging.soundThread.interrupt()");
						//morzeRinging.soundThread.interrupt();
						Log.d(TAG, "Вызывается сообщение morzeRinging.soundThread.isAlive = " + morzeRinging.soundThread.isAlive());
					}
				}
				break;
			case TelephonyManager.CALL_STATE_IDLE:
				Log.d(TAG, "Получено сообщение TelephonyManager.CALL_STATE_IDLE ");
				if (morzeRinging.soundThread != null) {
					if (morzeRinging.soundThread.isAlive()){
						morzeRinging.isRun=false;  //дать понять звуковому треду, чтобы он завершился
						Log.d(TAG, "Вызывается сообщение morzeRinging.soundThread.interrupt()");
						//morzeRinging.soundThread.interrupt();
						Log.d(TAG, "Вызывается сообщение morzeRinging.soundThread.isAlive = " + morzeRinging.soundThread.isAlive());
					}
				}
				break;

			}
		}
	}
	
	/**
	 * Broadcast receiver to detect the outgoing calls.
	 */
	public class OutgoingReceiver extends BroadcastReceiver {
	    public OutgoingReceiver() {
	    }

	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

			Toast.makeText(ctx,
					"Outgoing: " + number,
					Toast.LENGTH_LONG).show();
	    }
  
	}

	//метод получения имени абонента по номеру телефона
	public String getNameByPhoneNumber(String phonenumber){
		Log.d(TAG, "phonenumber = " + phonenumber);
		String name="";
		Cursor c = ctx.getContentResolver().
				query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						new String[]{Phone._ID, Phone.DISPLAY_NAME, Phone.NUMBER},
						Phone.NUMBER+" = '" + phonenumber + "'", null, null);

				/*Cursor c = ctx.getContentResolver().query(
						Contacts.CONTENT_URI,
						new String[] {Contacts._ID},
						Contacts.DISPLAY_NAME + " = 'Robert Smith'", null, null);*/
		Log.d(TAG, "Полученное значение курсор " + c.toString());
		Log.d(TAG, "Полученное значение c.getCount() " + c.getCount());
		Log.d(TAG, "Полученное значение c.getColumnCount() " + c.getColumnCount());
		//c.moveToNext();
		Log.d(TAG, "Полученное значение c.getPosition() " + c.getPosition());
		//Log.d(TAG, "Полученное значение c.getString(0) " + c.getString(1));
		int i=0;
		if (c.getCount() > 0)
		{
			while (c.moveToNext() && (i < 5))
			{
				// process them as you want
				Log.d(TAG," ID = "+c.getString(0)+" NAME = "+c.getString(1)+" PHONE = "+c.getString(2));
				name = c.getString(1);
				i++;
			}

		}
		Log.d(TAG,"Проверка прошло ли определение ");
		//Log.d(TAG,"name = " + name);
		return name;
	}

}
