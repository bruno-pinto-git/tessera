package com.tessera.android.data

import android.content.Context
import android.util.Log
import com.newland.nsdk.core.api.common.ModuleType
import com.newland.nsdk.core.api.internal.barcodescanner.BarcodeScanner
import com.newland.nsdk.core.internal.NSDKModuleManagerImpl

object NsdkRepository {

    private const val TAG = "NsdkRepository"

    /** True apenas num dispositivo Newland onde o NSDK arrancou com sucesso. */
    var available: Boolean = false
        private set

    /** Null num dispositivo sem o hardware/SDK Newland (ex: telemóvel normal). */
    val barcodeScanner: BarcodeScanner? by lazy {
        if (!available) {
            null
        } else {
            try {
                NSDKModuleManagerImpl.getInstance().getModule(ModuleType.BARCODE_SCANNER) as? BarcodeScanner
            } catch (t: Throwable) {
                Log.e(TAG, "Barcode scanner indisponível: ${t.message}", t)
                null
            }
        }
    }

    fun init(context: Context) {
        // catch Throwable: num telemóvel sem Newland o NSDK pode lançar Error
        // (UnsatisfiedLinkError/NoClassDefFoundError), que catch(Exception) não apanha.
        available = try {
            NSDKModuleManagerImpl.getInstance().init(context)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "NSDK indisponível neste dispositivo: ${t.message}", t)
            false
        }
    }

    fun destroy() {
        try {
            NSDKModuleManagerImpl.getInstance().destroy()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to destroy NSDK: ${t.message}", t)
        }
    }
}
