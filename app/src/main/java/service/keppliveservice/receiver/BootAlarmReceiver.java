package service.keppliveservice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import service.keppliveservice.service.OnLineService;


public class BootAlarmReceiver extends BroadcastReceiver {

	public BootAlarmReceiver() {

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("91ysdk", "开机广播");
		Intent startSrv = new Intent(context, OnLineService.class);
		startSrv.putExtra("CMD", "TICK");
		context.startService(startSrv);
	}

}
