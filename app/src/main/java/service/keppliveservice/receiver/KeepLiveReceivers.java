package service.keppliveservice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import service.keppliveservice.KeepLiveUtils;
import service.keppliveservice.service.OnLineService;

/**
 * 网络连接改变锁屏广播和解锁
 * Created by haifeng on 2017/8/28.
 */
public class KeepLiveReceivers extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i("91ysdk",action);
        if(KeepLiveUtils.hasNetwork(context) == false){
            return;
        }
        if (!KeepLiveUtils.isServiceWork(context,"service.keppliveservice.service.OnLineService")){
            Intent startSrv = new Intent(context, OnLineService.class);
            startSrv.putExtra("CMD", "TICK");
            context.startService(startSrv);
        }
    }
}
