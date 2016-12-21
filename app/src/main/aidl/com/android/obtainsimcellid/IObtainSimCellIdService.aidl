// IObtainSimCellIdService.aidl
package com.android.obtainsimcellid;

import com.android.obtainsimcellid.IServiceCallback;

// Declare any non-default types here with import statements

interface IObtainSimCellIdService {

    void startObtainCellId();
    void stopObtainCellId();
    boolean isStart();
    int getSim1CellId();
    int getSim2CellId();
    void setCallback(IServiceCallback callback);

}
