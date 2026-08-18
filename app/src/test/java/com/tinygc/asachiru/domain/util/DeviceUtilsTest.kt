package com.tinygc.asachiru.domain.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * DeviceUtilsのユニットテスト
 */
class DeviceUtilsTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPackageManager: PackageManager

    @Mock
    private lateinit var mockUiModeManager: UiModeManager

    @Mock
    private lateinit var mockResources: Resources

    @Mock
    private lateinit var mockConfiguration: Configuration

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockContext.resources).thenReturn(mockResources)
        `when`(mockResources.configuration).thenReturn(mockConfiguration)
    }

    @Test
    fun `isTV returns true when Leanback feature is available`() {
        // Given: Leanback機能が利用可能
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(true)
        `when`(mockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(mockUiModeManager)
        `when`(mockUiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_NORMAL)

        // When: isTV()を呼び出し
        val result = DeviceUtils.isTV(mockContext)

        // Then: trueが返る
        assertTrue(result)
    }

    @Test
    fun `isTV returns true when UI mode is TV`() {
        // Given: UIモードがTV
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(false)
        `when`(mockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(mockUiModeManager)
        `when`(mockUiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_TELEVISION)

        // When: isTV()を呼び出し
        val result = DeviceUtils.isTV(mockContext)

        // Then: trueが返る
        assertTrue(result)
    }

    @Test
    fun `isTV returns false when neither Leanback nor TV mode`() {
        // Given: LeanbackもTVモードもなし
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(false)
        `when`(mockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(mockUiModeManager)
        `when`(mockUiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_NORMAL)

        // When: isTV()を呼び出し
        val result = DeviceUtils.isTV(mockContext)

        // Then: falseが返る
        assertFalse(result)
    }

    @Test
    fun `isStrictTelevision returns true when Leanback feature and TV UI mode both present`() {
        // Given: Leanback機能ありかつUIモードがTV（実際のTVデバイス）
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(true)
        `when`(mockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(mockUiModeManager)
        `when`(mockUiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_TELEVISION)

        // When: isStrictTelevision()を呼び出し
        val result = DeviceUtils.isStrictTelevision(mockContext)

        // Then: trueが返る
        assertTrue(result)
    }

    @Test
    fun `isStrictTelevision returns false when UI mode reports TV but Leanback feature is absent`() {
        // Given: UIモードだけがTVを報告している（一部端末でスマホでも発生しうる誤報告）が
        //        Leanback機能を持たない = 実際にはTVではない
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(false)
        `when`(mockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(mockUiModeManager)
        `when`(mockUiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_TELEVISION)

        // When: isStrictTelevision()を呼び出し
        val result = DeviceUtils.isStrictTelevision(mockContext)

        // Then: falseが返る（エッジ ツー エッジ表示のスキップを誤って行わない）
        assertFalse(result)
    }

    @Test
    fun `isStrictTelevision returns false when Leanback feature present but UI mode is normal`() {
        // Given: Leanback機能はあるがUIモードは通常（TVモードに遷移していないタイミングなど）
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(true)
        `when`(mockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(mockUiModeManager)
        `when`(mockUiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_NORMAL)

        // When: isStrictTelevision()を呼び出し
        val result = DeviceUtils.isStrictTelevision(mockContext)

        // Then: falseが返る
        assertFalse(result)
    }

    @Test
    fun `isStrictTelevision returns false when neither Leanback nor TV UI mode`() {
        // Given: LeanbackもTVモードもなし
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(false)
        `when`(mockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(mockUiModeManager)
        `when`(mockUiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_NORMAL)

        // When: isStrictTelevision()を呼び出し
        val result = DeviceUtils.isStrictTelevision(mockContext)

        // Then: falseが返る
        assertFalse(result)
    }

    @Test
    fun `isStrictTelevision returns false when UiModeManager service is unavailable`() {
        // Given: UI_MODE_SERVICEが取得できない（一部端末で発生しうる）
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(true)
        `when`(mockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(null)

        // When: isStrictTelevision()を呼び出し
        val result = DeviceUtils.isStrictTelevision(mockContext)

        // Then: falseが返る（安全側=TVではないと判定し、edge-to-edgeを有効なままにする）
        assertFalse(result)
    }

    @Test
    fun `isPhone returns true when not TV and has touchscreen`() {
        // Given: TVではなく、タッチスクリーンあり
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(false)
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)).thenReturn(true)
        `when`(mockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(mockUiModeManager)
        `when`(mockUiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_NORMAL)

        // When: isPhone()を呼び出し
        val result = DeviceUtils.isPhone(mockContext)

        // Then: trueが返る
        assertTrue(result)
    }

    @Test
    fun `isPhone returns false when is TV`() {
        // Given: TVデバイス
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(true)
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)).thenReturn(true)
        `when`(mockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(mockUiModeManager)
        `when`(mockUiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_TELEVISION)

        // When: isPhone()を呼び出し
        val result = DeviceUtils.isPhone(mockContext)

        // Then: falseが返る
        assertFalse(result)
    }

    @Test
    fun `isPhone returns false when no touchscreen`() {
        // Given: TVではないが、タッチスクリーンなし
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(false)
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)).thenReturn(false)
        `when`(mockContext.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(mockUiModeManager)
        `when`(mockUiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_NORMAL)

        // When: isPhone()を呼び出し
        val result = DeviceUtils.isPhone(mockContext)

        // Then: falseが返る
        assertFalse(result)
    }

    @Test
    fun `hasTouchscreen returns true when touchscreen is available`() {
        // Given: タッチスクリーンあり
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)).thenReturn(true)

        // When: hasTouchscreen()を呼び出し
        val result = DeviceUtils.hasTouchscreen(mockContext)

        // Then: trueが返る
        assertTrue(result)
    }

    @Test
    fun `hasTouchscreen returns false when touchscreen is not available`() {
        // Given: タッチスクリーンなし
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)).thenReturn(false)

        // When: hasTouchscreen()を呼び出し
        val result = DeviceUtils.hasTouchscreen(mockContext)

        // Then: falseが返る
        assertFalse(result)
    }

    @Test
    fun `getScreenSizeCategory returns correct screen size`() {
        // Given: 画面サイズがLARGE
        mockConfiguration.screenLayout = Configuration.SCREENLAYOUT_SIZE_LARGE

        // When: getScreenSizeCategory()を呼び出し
        val result = DeviceUtils.getScreenSizeCategory(mockContext)

        // Then: SCREENLAYOUT_SIZE_LARGEが返る
        assertEquals(Configuration.SCREENLAYOUT_SIZE_LARGE, result)
    }

    @Test
    fun `isLargeScreen returns true when screen size is LARGE`() {
        // Given: 画面サイズがLARGE
        mockConfiguration.screenLayout = Configuration.SCREENLAYOUT_SIZE_LARGE

        // When: isLargeScreen()を呼び出し
        val result = DeviceUtils.isLargeScreen(mockContext)

        // Then: trueが返る
        assertTrue(result)
    }

    @Test
    fun `isLargeScreen returns true when screen size is XLARGE`() {
        // Given: 画面サイズがXLARGE
        mockConfiguration.screenLayout = Configuration.SCREENLAYOUT_SIZE_XLARGE

        // When: isLargeScreen()を呼び出し
        val result = DeviceUtils.isLargeScreen(mockContext)

        // Then: trueが返る
        assertTrue(result)
    }

    @Test
    fun `isLargeScreen returns false when screen size is NORMAL`() {
        // Given: 画面サイズがNORMAL
        mockConfiguration.screenLayout = Configuration.SCREENLAYOUT_SIZE_NORMAL

        // When: isLargeScreen()を呼び出し
        val result = DeviceUtils.isLargeScreen(mockContext)

        // Then: falseが返る
        assertFalse(result)
    }

    @Test
    fun `isLargeScreen returns false when screen size is SMALL`() {
        // Given: 画面サイズがSMALL
        mockConfiguration.screenLayout = Configuration.SCREENLAYOUT_SIZE_SMALL

        // When: isLargeScreen()を呼び出し
        val result = DeviceUtils.isLargeScreen(mockContext)

        // Then: falseが返る
        assertFalse(result)
    }
}
