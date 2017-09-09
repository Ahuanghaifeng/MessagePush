package service.keppliveservice.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.widget.Toast;

import service.keppliveservice.KeepLiveUtils;
import service.keppliveservice.StrongService;


/**
 * Created by haifeng on 2017/8/24.
 */

public class KeepLiveService extends Service {

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1:
                    startOnlineService();
                    break;

                default:
                    break;
            }

        };
    };

    @Override
    public void onCreate() {

        Toast.makeText(KeepLiveService.this, "Service2 启动中...", Toast.LENGTH_SHORT).show();
        startOnlineService();
        /*
         * 此线程用监听Service2的状态
         */
        new Thread() {
            public void run() {
                while (true) {
                    boolean isRun = KeepLiveUtils.isServiceWork(KeepLiveService.this,"service.keppliveservice.service.OnLineService");
                    if (!isRun) {
                        Message msg = Message.obtain();
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
    public IBinder onBind(Intent intent) {
        return (IBinder) startS1;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * 判断Service1是否还在运行，如果不是则启动Service1
     */
    private void startOnlineService() {
        boolean isRun = KeepLiveUtils.isServiceWork(KeepLiveService.this,
                "service.keppliveservice.service.OnLineService");
        if (isRun == false) {
            try {
                startS1.StartService();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 使用aidl 启动Service1
     */
    private StrongService startS1 = new StrongService.Stub() {

        @Override
        public void StartService() throws RemoteException {
            Intent i = new Intent(getBaseContext(), OnLineService.class);
            getBaseContext().startService(i);
        }

        @Override
        public void stopService() throws RemoteException {
            Intent i = new Intent(getBaseContext(), OnLineService.class);
            getBaseContext().stopService(i);
        }
    };

    @Override
    public void onTrimMemory(int level) {
        startOnlineService();
    }
}
