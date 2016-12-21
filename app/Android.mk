LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_CERTIFICATE := platform

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
	src/com/android/obtainsimcellid/IServiceCallback.aidl \
	src/com/android/obtainsimcellid/IObtainSimCellIdService.aidl

LOCAL_PACKAGE_NAME := ObtainSimCellId
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.cfg
LOCAL_PROGUARD_ENABLED := disabled
#LOCAL_PROGUARD_ENABLED := custom

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
#include $(call all-makefiles-under,$(LOCAL_PATH))
