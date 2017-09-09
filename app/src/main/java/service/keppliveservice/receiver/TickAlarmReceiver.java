package service.keppliveservice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import service.keppliveservice.KeepLiveUtils;
import service.keppliveservice.service.OnLineService;


public class TickAlarmReceiver extends BroadcastReceiver {

	WakeLock wakeLock;
	
	public TickAlarmReceiver() {
		
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("91ysdk", "收到闹钟广播");
		if(KeepLiveUtils.hasNetwork(context) == false){
			return;
		}
		Intent startSrv = new Intent(context, OnLineService.class);
		startSrv.putExtra("CMD", "TICK");
		context.startService(startSrv);
	}

}
