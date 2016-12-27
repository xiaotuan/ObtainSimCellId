ObtainSimCellIdService(获取sim卡基站Id)

===================================================================
应用名称：获取sim卡基站Id
版本号：V1.0
创建日期：2016-12-21
=============================================================

===================================================================
功能说明：
	（１）获取sim卡１的基站ID
	（２）获取sim卡２的基站ID
	（３）该应用只适用于MTK平台
=============================================================

===================================================================
技术解析：
	（１）获取基站ID的方法：
			GSM和CDMA的获取方法是不一样的，GSM基站ID的获取方法如下：
				if (mTelephonyManager.getCellLocation() instanceof GsmCellLocation) {
					GsmCellLocation location = (GsmCellLocation) mTelephonyManager.getCellLocation();
					cellid = location.getCid();
				}
			CDMA基站ID的获取方法如下：
				if (mTelephonyManager.getCellLocation() instanceof CdmaCellLocation) {
					CdmaCellLocation location = (CdmaCellLocation) mTelephonyManager.getCellLocation();
					cellid = location.getBaseStationId();
				}
	（２）如果手机插入两张SIM卡，则需要在获取基站ID时，先将要获取基站ID的sim卡设置为默认数据连接sim卡
	设置默认数据连接的方法请看ObtainSimCellIdService.java中的public boolean setDefaultDataSubId(int slotId)；方法。
=============================================================
********************************************************************************************************
=============================================================
版本号：V2.0
修改日期：2016-12-27 14:57
=============================================================

===================================================================
修改说明：
	（１）不用设置默认数据连接sim卡，就可以获取卡１和卡２的基站ID
=============================================================

===================================================================
功能说明：
	（１）获取基站ID的方法和版本V1.0一样，只是获取CellLocation的方法不一样。
这个版本是通过Phone对象的getCellLocation()进行获取的。而Phone对象又可以
通过PhoneFactory类的getPhone(int slotId)方法获取对应sim卡的Phone对象。
	（２）要使用Phone对象，则使用的类必须运行在com.android.phone进程中。
例如在AndroidManifest.xml中如此定义：
	<service
            android:name=".ObtainSimCellIdService"
            android:process="com.android.phone"
            android:enabled="true"
            android:exported="true" />
=============================================================
