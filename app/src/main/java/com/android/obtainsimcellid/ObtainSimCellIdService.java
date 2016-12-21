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
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

public class ObtainSimCellIdService extends Service {

    private static final int NOTIFY_ID = 10;

    private static final int MSG_GET_CELLID = 0;
    private static final int MSG_SET_CELLID = 1;
    private static final int MSG_STOP_SERVICE = 2;
    private static final int MSG_SET_DEFAULT_DATA_SUBID = 3;

    private static final int GET_CELLID_RETRYED_TIMES = 5;
    private static final int SET_DEFAULT_SUBID_RETRYED_TIMES = 5;
    private static final int STOP_SERVICE_DELAYED = 60000;

    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private IServiceCallback mCallback;

    private int mSubId;
    private int mSlotId;
    private int mLastSubId;
    private int mSim1CellId;
    private int mSim2CellId;
    private int mBinderCount = 0;
    private int mGetCellIdRetryTimes;
    private int mSetDefaultDataSubIdRetryTimes;
    private boolean mStarting;

    @Override
    public IBinder onBind(Intent intent) {
        mBinderCount++;
        return new MyBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBinderCount--;
        if (mBinderCount <= 0 && !mStarting) {
            mHandler.sendEmptyMessageDelayed(MSG_STOP_SERVICE, STOP_SERVICE_DELAYED);
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSubscriptionManager = SubscriptionManager.from(this);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mCallback = null;

        mSubId = -1;
        mSlotId = -1;
        mLastSubId = -1;
        mSim1CellId = -1;
        mSim2CellId = -1;
        mGetCellIdRetryTimes = 0;
        mSetDefaultDataSubIdRetryTimes = 0;
        mStarting = false;
        Notification notify = new Notification();
        notify.flags = Notification.FLAG_FOREGROUND_SERVICE;
        startForeground(NOTIFY_ID, notify);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    private int getCellId() {
        int cellid = -1;

        if (mTelephonyManager.getCellLocation() instanceof GsmCellLocation) {
            GsmCellLocation location = (GsmCellLocation) mTelephonyManager.getCellLocation();
            cellid = location.getCid();
        } else if (mTelephonyManager.getCellLocation() instanceof CdmaCellLocation) {
            CdmaCellLocation location = (CdmaCellLocation) mTelephonyManager.getCellLocation();
            cellid = location.getBaseStationId();
        }
        return cellid;
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

    public boolean setDefaultDataSubId(int slotId) {
        boolean result = false;
        int defaultSubId = mSubscriptionManager.getDefaultDataSubId();
        int defaultSlotId = mSubscriptionManager.getSlotId(defaultSubId);
        SubscriptionInfo subInfo = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slotId);
        if (subInfo != null) {
            mSubId = subInfo.getSubscriptionId();
            mSubscriptionManager.setDefaultDataSubId(mSubId);
            //mTelephonyManager.setDataEnabled(true);
            result = true;
        }
        Log.d(this, "setDefaultDataSubId=>result: " + result + " subId: " + mSubId + " slotId: " + slotId);
        return result;
    }

    private void simCellIdChanged(int slotId, int cellId) {
        Log.d(this, "simCellIdChanged=>slotId: " + slotId + " cellId: " + cellId);
        if (mCallback != null) {
            try {
                mCallback.onSimCellIdChanged(slotId, cellId);
            } catch (RemoteException e) {
                Log.e(this, "simCellIdChanged=>error: ", e);
            }
        }
    }

    private void SimCellIdObtainCompleted() {
        mHandler.removeMessages(MSG_SET_CELLID);
        mHandler.removeMessages(MSG_GET_CELLID);
        mStarting = false;
        if (mCallback != null) {
            try {
                mCallback.onStateChanged(mStarting);
            } catch (RemoteException e) {
                Log.e(this, "SimCellIdObtainCompleted=>error: ", e);
            }
        }
        if (mLastSubId != -1 && mLastSubId !=  mSubscriptionManager.getDefaultDataSubId()) {
            mSubscriptionManager.setDefaultDataSubId(mLastSubId);
        }
        if (mBinderCount <= 0 && !mStarting) {
            mHandler.sendEmptyMessageDelayed(MSG_STOP_SERVICE, STOP_SERVICE_DELAYED);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_CELLID:
                    mHandler.removeMessages(MSG_GET_CELLID);
                    if (mSlotId == -1) {
                        mSlotId = 0;
                        if (!isSimExist(mSlotId)) {
                            simCellIdChanged(mSlotId, -1);
                            mSlotId = 1;
                            if (!isSimExist(mSlotId)) {
                                simCellIdChanged(mSlotId, -1);
                                mSlotId = -1;
                            }
                        }
                    } else {
                        mSlotId = 1;
                        if (!isSimExist(mSlotId)) {
                            simCellIdChanged(mSlotId, -1);
                            mSlotId = -1;
                        }
                    }
                    Log.d(ObtainSimCellIdService.this, "handleMessage(MSG_GET_CELLID)=>soltId: " + mSlotId);
                    if (mSlotId != -1) {
                        mGetCellIdRetryTimes = 0;
                        mSetDefaultDataSubIdRetryTimes = 0;
                        mHandler.sendEmptyMessage(MSG_SET_DEFAULT_DATA_SUBID);
                    } else {
                        SimCellIdObtainCompleted();
                    }
                    break;

                case MSG_SET_CELLID:
                    mHandler.removeMessages(MSG_SET_CELLID);
                    Log.d(ObtainSimCellIdService.this, "handleMessage(MSG_SET_CELLID)=>soltId: " + mSlotId);
                    if (mSlotId != -1) {
                        int defaultSubId = mSubscriptionManager.getDefaultDataSubId();
                        Log.d(ObtainSimCellIdService.this, "handleMessage(MSG_SET_CELLID)=>defaultSubId: " + defaultSubId + " subId: " + mSubId);
                        if (mSubId != defaultSubId) {
                            mSetDefaultDataSubIdRetryTimes = 0;
                            mHandler.sendEmptyMessage(MSG_SET_DEFAULT_DATA_SUBID);
                        } else {
                            int cellId = getCellId();
                            Log.d(ObtainSimCellIdService.this, "handleMessage(MSG_SET_CELLID)=>cellId: " + cellId);
                            if (cellId <= 0) {
                                Log.d(ObtainSimCellIdService.this, "handleMessage(MSG_SET_CELLID)=>mGetCellIdRetryTimes: " + mGetCellIdRetryTimes);
                                if (mGetCellIdRetryTimes < GET_CELLID_RETRYED_TIMES) {
                                    mHandler.sendEmptyMessageDelayed(MSG_SET_CELLID, 20000);
                                    mGetCellIdRetryTimes++;
                                } else {
                                    if (mSlotId == 0) {
                                        simCellIdChanged(mSlotId, -1);
                                        mHandler.sendEmptyMessage(MSG_GET_CELLID);
                                    } else {
                                        simCellIdChanged(mSlotId, -1);
                                        mSlotId = -1;
                                        SimCellIdObtainCompleted();
                                    }
                                }
                            } else {
                                Log.d(ObtainSimCellIdService.this, "handleMessage(MSG_SET_CELLID)=>soltId: " + mSlotId + " cellId: " + cellId);
                                if (mSlotId == 0) {
                                    mSim1CellId = cellId;
                                    simCellIdChanged(mSlotId, cellId);
                                    mHandler.sendEmptyMessage(MSG_GET_CELLID);
                                } else {
                                    mSim2CellId = cellId;
                                    simCellIdChanged(mSlotId, cellId);
                                    mSlotId = -1;
                                    SimCellIdObtainCompleted();
                                }
                            }
                        }
                    }
                    break;

                case MSG_STOP_SERVICE:
                    stopForeground(true);
                    stopSelf();
                    break;

                case MSG_SET_DEFAULT_DATA_SUBID:
                    boolean success = setDefaultDataSubId(mSlotId);
                    if (success) {
                        mHandler.sendEmptyMessageDelayed(MSG_SET_CELLID, 60000);
                    } else {
                        if (mSetDefaultDataSubIdRetryTimes < SET_DEFAULT_SUBID_RETRYED_TIMES) {
                            mHandler.sendEmptyMessageDelayed(MSG_SET_DEFAULT_DATA_SUBID, 60000);
                            mSetDefaultDataSubIdRetryTimes++;
                        } else {
                            simCellIdChanged(mSlotId, -1);
                            mHandler.sendEmptyMessage(MSG_GET_CELLID);
                        }
                    }
                    break;
            }
        }
    };

    class MyBinder extends IObtainSimCellIdService.Stub {

        @Override
        public void startObtainCellId() throws RemoteException {
            mSlotId = -1;
            mSim2CellId = -1;
            mSim1CellId = -1;
            mLastSubId = mSubscriptionManager.getDefaultDataSubId();
            mHandler.sendEmptyMessage(MSG_GET_CELLID);
            mStarting = true;
        }

        @Override
        public void stopObtainCellId() throws RemoteException {
            mHandler.removeMessages(MSG_SET_CELLID);
            mHandler.removeMessages(MSG_GET_CELLID);
            mStarting = false;
        }

        @Override
        public boolean isStart() throws RemoteException {
            return mStarting;
        }

        @Override
        public int getSim1CellId() throws RemoteException {
            return mSim1CellId;
        }

        @Override
        public int getSim2CellId() throws RemoteException {
            return mSim2CellId;
        }

        @Override
        public void setCallback(IServiceCallback callback) throws RemoteException {
            mCallback = callback;
            if (callback != null) {
                callback.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        mCallback = null;
                    }
                }, 0);
            }
        }
    }
}
