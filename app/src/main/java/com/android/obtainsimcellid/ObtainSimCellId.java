package com.android.obtainsimcellid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ObtainSimCellId extends Activity implements View.OnClickListener {

    private TextView mSim1CellId;
    private TextView mSim2CellId;
    private Button mRefreshBt;
    private IObtainSimCellIdService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obtain_sim_cell_id);

        mSim1CellId = (TextView) findViewById(R.id.brand);
        mSim2CellId = (TextView) findViewById(R.id.model);
        mRefreshBt = (Button) findViewById(R.id.refresh);
        mRefreshBt.setOnClickListener(this);
        bindService();
        Log.d(this, "onCreate()...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(this, "onResume()...");
        updateViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(this, "onPause()...");
    }

    @Override
    protected void onDestroy() {
        Log.d(this, "onDestroy()...");
        unbindService(mConnection);
        super.onDestroy();
    }

    private void bindService() {
        Intent service = new Intent(this, ObtainSimCellIdService.class);
        startService(service);
        bindService(service, mConnection, BIND_AUTO_CREATE);
    }

    private void updateViews() {
        if (mService == null) {
            mSim1CellId.setText("SIM1 CellId: " + getString(R.string.unknown_cell_id));
            mSim2CellId.setText("SIM2 CellId: " + getString(R.string.unknown_cell_id));
        } else {
            try {
                mSim1CellId.setText("SIM1 CellId: " + (mService.getSim1CellId() <= 0 ? getString(R.string.unknown_cell_id) : mService.getSim1CellId()));
                mSim2CellId.setText("SIM2 CellId: " + (mService.getSim2CellId() <= 0 ? getString(R.string.unknown_cell_id) : mService.getSim2CellId()));
            } catch (RemoteException e) {
                Log.e(this, "updateViews=>error: ", e);
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(ObtainSimCellId.this, "onServiceConnected()...");
            mService = IObtainSimCellIdService.Stub.asInterface(service);
            updateViews();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(ObtainSimCellId.this, "onServiceDisconnected()...");
            mService = null;
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.refresh:
                updateViews();
                break;
        }
    }
}
