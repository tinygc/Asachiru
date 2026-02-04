package com.tinygc.asachiru.domain.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * QRコード生成ユーティリティ
 *
 * Google Play StoreのURLからQRコードを生成します。
 */
object QrCodeGenerator {

    /**
     * Play Store URLの定数
     */
    object PlayStoreUrls {
        const val ASACHIRU = "https://play.google.com/store/apps/details?id=com.tinygc.asachiru"
        const val FEEDWATCH = "https://play.google.com/store/apps/details?id=com.tinygc.feedwatch"
    }

    /**
     * テキストからQRコードのBitmapを生成します（同期版）
     *
     * @param text QRコードに含めるテキスト（URL等）
     * @param size QRコードのピクセルサイズ（デフォルト: 200px）
     * @return 生成されたQRコードのBitmap、失敗時はnull
     */
    fun generateQrCode(text: String, size: Int = 200): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            bitmap
        } catch (e: Exception) {
            android.util.Log.e("QrCodeGenerator", "Failed to generate QR code", e)
            null
        }
    }

    /**
     * テキストからQRコードのBitmapを非同期で生成します
     *
     * @param text QRコードに含めるテキスト（URL等）
     * @param size QRコードのピクセルサイズ（デフォルト: 200px）
     * @return 生成されたQRコードのBitmap、失敗時はnull
     */
    suspend fun generateQrCodeAsync(text: String, size: Int = 200): Bitmap? = withContext(Dispatchers.Default) {
        generateQrCode(text, size)
    }
}
