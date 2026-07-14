package com.isaacshub.app.routehelper.ui.common

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import java.io.File

/** Shared osmdroid setup: persistent tile cache under filesDir so previously-viewed tiles work offline. */
fun newOsmMapView(context: Context): MapView {
    Configuration.getInstance().userAgentValue = context.packageName
    Configuration.getInstance().osmdroidBasePath = File(context.filesDir, "osmdroid")
    Configuration.getInstance().osmdroidTileCache = File(context.filesDir, "osmdroid/tiles")
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        controller.setZoom(18.0)
    }
}
