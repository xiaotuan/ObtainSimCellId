package com.android.obtainsimcellid;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.CellLocation;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class ObtainSimCellIdService extends Service {

    private static final int NOTIFY_ID = 10;

    private static final int MSG_STOP_SERVICE = 0;

    private static final int STOP_SERVICE_DELAYED = 60000;

    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;

    private int mBinderCount = 0;

    @Override
    public IBinder onBind(Intent intent) {
        mHandler.removeMessages(MSG_STOP_SERVICE);
        mBinderCount++;
        Log.d(this, "onBind=>count: " + mBinderCount);
        return new MyBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBinderCount--;
        if (mBinderCount <= 0) {
            mBinderCount = 0;
            mHandler.sendEmptyMessageDelayed(MSG_STOP_SERVICE, STOP_SERVICE_DELAYED);
        }
        Log.d(this, "onUnbind=>count: " + mBinderCount);
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(this, "onCreate()...");
        mSubscriptionManager = SubscriptionManager.from(this);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mBinderCount = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(this, "onStartCommand=>count: " + mBinderCount);
        if (mBinderCount <= 0) {
            mHandler.sendEmptyMessageDelayed(MSG_STOP_SERVICE, STOP_SERVICE_DELAYED);
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(this, "onDestroy()...");
        super.onDestroy();
    }

    private int getCellIdBySlotId(int slotId) {
        int cellId = -1;
        Phone phone = PhoneFactory.getPhone(slotId);
        if (phone != null) {
            CellLocation cellLocation = phone.getCellLocation();
            if (cellLocation instanceof GsmCellLocation) {
                GsmCellLocation localGsmCellLocation = (GsmCellLocation) cellLocation;
                cellId = localGsmCellLocation.getCid();
            } else if (cellLocation instanceof CdmaCellLocation) {
                CdmaCellLocation localCdmaCellLocation = (CdmaCellLocation) cellLocation;
                cellId = localCdmaCellLocation.getBaseStationId();
            }
        }
        Log.d(this, "getCellIdBySlotId=>cellId: " + cellId + " slotId: " + slotId + " phone: " + phone);
        return cellId;
    }

    private boolean isSimExist(int slotId) {
        boolean exist = false;
        SubscriptionInfo subInfo = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slotId);
        if (subInfo != null) {
            exist = true;
        }
        Log.d(this, "isSimExist=>exist: " + exist + " slotId: " + slotId);
        return exist;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(ObtainSimCellIdService.this, "handleMessage=>what: " + msg.what);
            switch (msg.what) {
                case MSG_STOP_SERVICE:
                    stopSelf();
                    break;
            }
        }
    };

    class MyBinder extends IObtainSimCellIdService.Stub {

        @Override
        public int getSim1CellId() throws RemoteException {
            if (isSimExist(0)) {
                return getCellIdBySlotId(0);
            } else {
                return -1;
            }
        }

        @Override
        public int getSim2CellId() throws RemoteException {
            if (isSimExist(1)) {
                return getCellIdBySlotId(1);
            } else {
                return -1;
            }
        }

    }
}
