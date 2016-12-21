// IServiceCallback.aidl
package com.android.obtainsimcellid;

// Declare any non-default types here with import statements

interface IServiceCallback {

    void onSimCellIdChanged(int slotId, int cellId);
    void onStateChanged(boolean starting);

}
