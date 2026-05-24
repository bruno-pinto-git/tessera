package com.tessera.android.data

import android.content.Context
import android.util.Log
import com.newland.nsdk.core.api.common.ModuleType
import com.newland.nsdk.core.api.internal.barcodescanner.BarcodeScanner
import com.newland.nsdk.core.internal.NSDKModuleManagerImpl

object NsdkRepository {

    private const val TAG = "NsdkRepository"

    val barcodeScanner: BarcodeScanner by lazy {
        NSDKModuleManagerImpl.getInstance()
            .getModule(ModuleType.BARCODE_SCANNER) as BarcodeScanner
    }

    fun init(context: Context) {
        try {
            NSDKModuleManagerImpl.getInstance().init(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init NSDK: ${e.message}", e)
        }
    }

    fun destroy() {
        try {
            NSDKModuleManagerImpl.getInstance().destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to destroy NSDK: ${e.message}", e)
        }
    }
}
