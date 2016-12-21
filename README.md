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
