package service.keppliveservice.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.widget.Toast;

import org.ddpush.im.v1.client.appuser.Message;
import org.ddpush.im.v1.client.appuser.UDPClientBase;

import java.nio.ByteBuffer;

import service.keppliveservice.DateTimeUtil;
import service.keppliveservice.KeepLiveUtils;
import service.keppliveservice.MainActivity;
import service.keppliveservice.Params;
import service.keppliveservice.R;
import service.keppliveservice.StrongService;
import service.keppliveservice.Util;
import service.keppliveservice.receiver.TickAlarmReceiver;

public class OnLineService extends Service {

	protected PendingIntent tickPendIntent;
	protected TickAlarmReceiver tickAlarmReceiver = new TickAlarmReceiver();
	WakeLock wakeLock;
	MyUdpClient myUdpClient;
	Notification n;
	public static OnLineService mOnLineService;
	
	public class MyUdpClient extends UDPClientBase {

		public MyUdpClient(byte[] uuid, int appid, String serverAddr, int serverPort)
				throws Exception {
			super(uuid, appid, serverAddr, serverPort);

		}

		@Override
		public boolean hasNetworkConnection() {
			return Util.hasNetwork(OnLineService.this);
		}


		@Override
		public void trySystemSleep() {
			tryReleaseWakeLock();
		}

		@Override
		public void onPushMessage(Message message) {
			if(message == null){
				return;
			}
			if(message.getData() == null || message.getData().length == 0){
				return;
			}
			if(message.getCmd() == 16){// 0x10 通用推送信息
				notifyUser(16,"DDPush通用推送信息","时间："+ DateTimeUtil.getCurDateTime(),"收到通用推送信息");
			}
			if(message.getCmd() == 17){// 0x11 分组推送信息
				long msg = ByteBuffer.wrap(message.getData(), 5, 8).getLong();
				notifyUser(17,"DDPush分组推送信息",""+msg,"收到通用推送信息");
			}
			if(message.getCmd() == 32){// 0x20 自定义推送信息
				String str = null;
				try{
					str = new String(message.getData(),5,message.getContentLength(), "UTF-8");
				}catch(Exception e){
					str = Util.convert(message.getData(),5,message.getContentLength());
				}
				notifyUser(32,"DDPush自定义推送信息",""+str,"收到自定义推送信息");
			}
			setPkgsInfo();
		}

	}

	public OnLineService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.setTickAlarm();

		PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OnLineService");

		resetClient();

		notifyRunning();
		mOnLineService = this;

		Toast.makeText(OnLineService.this, "Service1 启动中...", Toast.LENGTH_SHORT).show();
		startKeepLiveService();
        /*
         * 此线程用监听Service2的状态
         */
		new Thread() {
			public void run() {
				while (true) {
					boolean isRun = KeepLiveUtils.isServiceWork(OnLineService.this,"service.keppliveservice.service.KeepLiveService");
					if (!isRun) {
						android.os.Message msg = android.os.Message.obtain();
						msg.what = 1;
						handler.sendMessage(msg);
					}
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	@Override
	public int onStartCommand(Intent param, int flags, int startId) {
		if(param == null){
			return START_STICKY;
		}
		String cmd = param.getStringExtra("CMD");
		if(cmd == null){
			cmd = "";
		}
		if(cmd.equals("TICK")){
			if(wakeLock != null && wakeLock.isHeld() == false){
				wakeLock.acquire();
			}
		}
		if(cmd.equals("RESET")){
			if(wakeLock != null && wakeLock.isHeld() == false){
				wakeLock.acquire();
			}
			resetClient();
		}
		if(cmd.equals("TOAST")){
			String text = param.getStringExtra("TEXT");
			if(text != null && text.trim().length() != 0){
				Toast.makeText(this, text, Toast.LENGTH_LONG).show();
			}
		}

		setPkgsInfo();
		return START_STICKY;
	}
	
	protected void setPkgsInfo(){
		if(this.myUdpClient == null){
			return;
		}
		long sent = myUdpClient.getSentPackets();
		long received = myUdpClient.getReceivedPackets();
		SharedPreferences account = this.getSharedPreferences(Params.DEFAULT_PRE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = account.edit();
		editor.putString(Params.SENT_PKGS, ""+sent);
		editor.putString(Params.RECEIVE_PKGS, ""+received);
		editor.commit();
	}

	protected void resetClient(){
		SharedPreferences account = this.getSharedPreferences(Params.DEFAULT_PRE_NAME, Context.MODE_PRIVATE);
		String serverIp = account.getString(Params.SERVER_IP, "");
		String serverPort = account.getString(Params.SERVER_PORT, "");
		String pushPort = account.getString(Params.PUSH_PORT, "");
		String userName = account.getString(Params.USER_NAME, "");
		if(serverIp == null || serverIp.trim().length() == 0
				|| serverPort == null || serverPort.trim().length() == 0
				|| pushPort == null || pushPort.trim().length() == 0
				|| userName == null || userName.trim().length() == 0){
			return;
		}
		if(this.myUdpClient != null){
			try{myUdpClient.stop();}catch(Exception e){}
		}
		try{
			myUdpClient = new MyUdpClient(Util.md5Byte(userName), 1, serverIp, Integer.parseInt(serverPort));
			myUdpClient.setHeartbeatInterval(50);
			myUdpClient.start();
			SharedPreferences.Editor editor = account.edit();
			editor.putString(Params.SENT_PKGS, "0");
			editor.putString(Params.RECEIVE_PKGS, "0");
			editor.commit();
		}catch(Exception e){
			Toast.makeText(this.getApplicationContext(), "操作失败："+e.getMessage(), Toast.LENGTH_LONG).show();
		}
		Toast.makeText(this.getApplicationContext(), "ddpush：终端重置", Toast.LENGTH_LONG).show();
	}

	protected void tryReleaseWakeLock(){
		if(wakeLock != null && wakeLock.isHeld() == true){
			wakeLock.release();
		}
	}

	protected void setTickAlarm(){
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(this,TickAlarmReceiver.class);
		int requestCode = 0;
		tickPendIntent = PendingIntent.getBroadcast(this,
		requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		//小米2s的MIUI操作系统，目前最短广播间隔为5分钟，少于5分钟的alarm会等到5分钟再触发！2014-04-28
		long triggerAtTime = System.currentTimeMillis();
		int interval = 300 * 1000;
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtTime, interval, tickPendIntent);
	}

	protected void cancelTickAlarm(){
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmMgr.cancel(tickPendIntent);
	}

	//将通知注释掉，去掉隐式通知
	protected void notifyRunning(){
//		NotificationManager notificationManager=(NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
//		n = new Notification();
//		Intent intent = new Intent(this,MainActivity.class);
//		PendingIntent pi = PendingIntent.getActivity(this, 0,intent, PendingIntent.FLAG_ONE_SHOT);
//		n.contentIntent = pi;
//		n.setLatestEventInfo(this, "DDPushDemoUDP", "正在运行", pi);
//		//n.defaults = Notification.DEFAULT_ALL;
//		//n.flags |= Notification.FLAG_SHOW_LIGHTS;
//		//n.flags |= Notification.FLAG_AUTO_CANCEL;
//		n.flags |= Notification.FLAG_ONGOING_EVENT;
//		n.flags |= Notification.FLAG_NO_CLEAR;
//		//n.iconLevel = 5;
//
//		n.icon = R.drawable.ic_launcher;
//		n.when = System.currentTimeMillis();
//		n.tickerText = "DDPushDemoUDP正在运行";
//		notificationManager.notify(0, n);
	}

	protected void cancelNotifyRunning(){
		NotificationManager notificationManager=(NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(0);
	}

	public void notifyUser(int id, String title, String content, String tickerText){
		NotificationManager notificationManager=(NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification n = new Notification();
		Intent intent = new Intent(this,MainActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0,intent, PendingIntent.FLAG_ONE_SHOT);
		n.contentIntent = pi;

//		n.setLatestEventInfo(this, title, content, pi);
		n.defaults = Notification.DEFAULT_ALL;
		n.flags |= Notification.FLAG_SHOW_LIGHTS;
		n.flags |= Notification.FLAG_AUTO_CANCEL;

		n.icon = R.mipmap.ic_launcher;
		n.when = System.currentTimeMillis();
		n.tickerText = tickerText;
		notificationManager.notify(id, n);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//this.cancelTickAlarm();
//		cancelNotifyRunning();
		this.tryReleaseWakeLock();
	}

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case 1:
					startKeepLiveService();
					break;

				default:
					break;
			}

		};
	};

	/**
	 * 使用aidl 启动Service2
	 */
	private StrongService startS2 = new StrongService.Stub() {

		@Override
		public void stopService() throws RemoteException {
			Intent i = new Intent(getBaseContext(), KeepLiveService.class);
			getBaseContext().stopService(i);
		}

		@Override
		public void StartService() throws RemoteException {
			Intent i = new Intent(getBaseContext(), KeepLiveService.class);
			getBaseContext().startService(i);
		}
	};

	/**
	 * 在内存紧张的时候，系统回收内存时，会回调OnTrimMemory， 重写onTrimMemory当系统清理内存时从新启动Service2
	 */
	@Override
	public void onTrimMemory(int level) {
		startKeepLiveService();
	}

	/**
	 * 判断Service2是否还在运行，如果不是则启动Service2
	 */
	private void startKeepLiveService() {
		boolean isRun = KeepLiveUtils.isServiceWork(OnLineService.this,
				"service.keppliveservice.service.KeepLiveService");
		if (isRun == false) {
			try {
				startS2.StartService();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return (IBinder) startS2;
	}
}
