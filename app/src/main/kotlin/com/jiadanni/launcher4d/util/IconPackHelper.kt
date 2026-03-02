package com.jiadanni.launcher4d.util

import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import com.jiadanni.launcher4d.model.App
import org.xmlpull.v1.XmlPullParser

object IconPackHelper {

    @JvmStatic
    fun applyIconPack(appManager: AppManager, iconSize: Int, iconPackName: String, apps: List<App>) {
        var iconPackResources: Resources? = null
        var intResourceIcon = 0
        var intResourceBack = 0
        var intResourceMask = 0
        var intResourceUpon = 0
        var scale = 1f

        val p = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
        }

        val origP = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
        }

        val maskP = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }

        if (iconPackName.isNotEmpty()) {
            try {
                iconPackResources = appManager.packageManager.getResourcesForApplication(iconPackName)
            } catch (e: Exception) {
                println(e)
            }

            iconPackResources?.let { resources ->
                getResource(resources, iconPackName, "iconback", null)?.let {
                    intResourceBack = resources.getIdentifier(it, "drawable", iconPackName)
                }
                getResource(resources, iconPackName, "iconmask", null)?.let {
                    intResourceMask = resources.getIdentifier(it, "drawable", iconPackName)
                }
                getResource(resources, iconPackName, "iconupon", null)?.let {
                    intResourceUpon = resources.getIdentifier(it, "drawable", iconPackName)
                }
                getResource(resources, iconPackName, "scale", null)?.let {
                    scale = it.toFloat()
                }
            }
        }

        val uniformOptions = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
            inDither = false
        }

        var back: Bitmap? = null
        var mask: Bitmap? = null
        var upon: Bitmap? = null

        if (iconPackName.isNotEmpty() && iconPackResources != null) {
            try {
                if (intResourceBack != 0) {
                    back = BitmapFactory.decodeResource(iconPackResources, intResourceBack, uniformOptions)
                }
                if (intResourceMask != 0) {
                    mask = BitmapFactory.decodeResource(iconPackResources, intResourceMask, uniformOptions)
                }
                if (intResourceUpon != 0) {
                    upon = BitmapFactory.decodeResource(iconPackResources, intResourceUpon, uniformOptions)
                }
            } catch (e: Exception) {
                println(e)
            }
        }

        for (i in apps.indices) {
            val app = apps[i]

            iconPackResources?.let { resources ->
                val iconResource = getResource(resources, iconPackName, null, app.componentName)
                intResourceIcon = if (iconResource != null) {
                    resources.getIdentifier(iconResource, "drawable", iconPackName)
                } else {
                    0
                }

                if (intResourceIcon != 0) {
                    // has single drawable for app
                    app.icon = BitmapDrawable(
                        BitmapFactory.decodeResource(resources, intResourceIcon, uniformOptions)
                    )
                } else {
                    try {
                        val orig = Bitmap.createBitmap(
                            app.icon.intrinsicWidth,
                            app.icon.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )

                        app.icon.setBounds(0, 0, app.icon.intrinsicWidth, app.icon.intrinsicHeight)
                        app.icon.draw(Canvas(orig))

                        val scaledOrig = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                        val scaledBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(scaledBitmap)

                        back?.let { canvas.drawBitmap(it, getResizedMatrix(it, iconSize, iconSize), p) }

                        val canvasOrig = Canvas(scaledOrig)
                        val resizedOrig = getResizedBitmap(orig, (iconSize * scale).toInt(), (iconSize * scale).toInt())
                        canvasOrig.drawBitmap(
                            resizedOrig,
                            (scaledOrig.width - resizedOrig.width / 2 - scaledOrig.width / 2).toFloat(),
                            (scaledOrig.width - resizedOrig.width / 2 - scaledOrig.width / 2).toFloat(),
                            origP
                        )

                        mask?.let { canvasOrig.drawBitmap(it, getResizedMatrix(it, iconSize, iconSize), maskP) }

                        canvas.drawBitmap(getResizedBitmap(scaledOrig, iconSize, iconSize), 0f, 0f, p)

                        upon?.let { canvas.drawBitmap(it, getResizedMatrix(it, iconSize, iconSize), p) }

                        app.icon = BitmapDrawable(appManager.context.resources, scaledBitmap)
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        }
    }

    private fun getResource(
        resources: Resources,
        packageName: String,
        resourceName: String?,
        componentName: String?
    ): String? {
        var resource: String? = null
        try {
            val resourceValue = resources.getIdentifier("appfilter", "xml", packageName)
            if (resourceValue != 0) {
                val xrp = resources.getXml(resourceValue)
                while (xrp.eventType != XmlResourceParser.END_DOCUMENT) {
                    if (xrp.eventType == XmlPullParser.START_TAG) {
                        try {
                            val string = xrp.name
                            if (componentName != null) {
                                if (xrp.getAttributeValue(0) == componentName) {
                                    resource = xrp.getAttributeValue(1)
                                }
                            } else if (string == resourceName) {
                                resource = xrp.getAttributeValue(0)
                            }
                        } catch (e: Exception) {
                            println(e)
                        }
                    }
                    xrp.next()
                }
            }
        } catch (e: Exception) {
            println(e)
        }
        return resource
    }

    private fun getResizedBitmap(bm: Bitmap, newHeight: Int, newWidth: Int): Bitmap {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = Matrix().apply {
            postScale(scaleWidth, scaleHeight)
        }
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true)
    }

    private fun getResizedMatrix(bm: Bitmap, newHeight: Int, newWidth: Int): Matrix {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        return Matrix().apply {
            postScale(scaleWidth, scaleHeight)
        }
    }
}
