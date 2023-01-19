package com.jacekpietras.mapview.logic

import android.graphics.Matrix
import com.jacekpietras.geometry.PointD
import com.jacekpietras.mapview.logic.ItemVisibility.CACHED
import com.jacekpietras.mapview.logic.ItemVisibility.HIDDEN
import com.jacekpietras.mapview.logic.PreparedItem.PreparedBitmapItem
import com.jacekpietras.mapview.logic.PreparedItem.PreparedColoredItem.PreparedCircleItem
import com.jacekpietras.mapview.logic.PreparedItem.PreparedColoredItem.PreparedPathItem
import com.jacekpietras.mapview.logic.PreparedItem.PreparedColoredItem.PreparedPolygonItem
import com.jacekpietras.mapview.logic.PreparedItem.PreparedIconItem
import com.jacekpietras.mapview.model.MapDimension
import com.jacekpietras.mapview.model.PaintHolder
import com.jacekpietras.mapview.model.RenderItem
import com.jacekpietras.mapview.model.ViewCoordinates
import com.jacekpietras.mapview.ui.LastMapUpdate
import timber.log.Timber

internal class RenderListMaker<T>(
    private val visibleGpsCoordinate: ViewCoordinates,
    private val worldRotation: Float,
    private val currentWidth: Int,
    private val currentHeight: Int,
    private val zoom: Double,
    private val centerGpsCoordinate: PointD,
    private val bakeDimension: (MapDimension) -> ((Double, PointD, Int) -> Float),
) {

    private val borders = mutableListOf<RenderItem<T>>()
    private val insides = mutableListOf<RenderItem<T>>()
    private val icons = mutableListOf<RenderItem.PointItem<T>>()
    private val dynamicPaints = mutableMapOf<PaintHolder.Dynamic<T>, T>()
    private val dynamicDimensions = mutableMapOf<MapDimension, Float>()
    private val matrix = Matrix()
        .apply {
            setRotate(
                -worldRotation,
                currentWidth / 2.toFloat(),
                currentHeight / 2.toFloat(),
            )
        }

    private var calculated: Int = 0
    private var skipped: Int = 0
    private var hidden: Int = 0

    fun translate(vararg preparedLists: List<PreparedItem<T>>): List<RenderItem<T>> {
        preparedLists.forEach(::addToRenderItems)
        Timber.d("Perf: skipped: $skipped, hidden: $hidden, calculated $calculated")
        LastMapUpdate.sortS = System.nanoTime()
        icons.sortBy { it.cY }
        LastMapUpdate.sortE = System.nanoTime()
        return borders + insides + icons
    }

    private fun addToRenderItems(preparedList: List<PreparedItem<T>>) {
        preparedList.forEach { item ->
            if (item.visibility != HIDDEN) {
                when (item) {
                    is PreparedPolygonItem -> {
                        if (item.visibility == CACHED) {
                            item.addToRender(item.cacheTranslated!!)
                            skipped++
                        } else if (item.minZoom.isBiggerThanZoom()) {
                            item.shape
                                .let(visibleGpsCoordinate::transformPolygon)
                                .withMatrix(matrix, worldRotation)
                                .let { polygon ->
                                    item.cacheTranslated = polygon
                                    item.visibility = CACHED
                                    item.addToRender(polygon)
                                    calculated++
                                }
                        }
                    }
                    is PreparedPathItem -> {
                        if (item.visibility == CACHED) {
                            item.cacheTranslated!!.forEach { item.addToRender(it) }
                            skipped++
                        } else if (item.minZoom.isBiggerThanZoom()) {
                            item.cacheRaw!!
                                .let(visibleGpsCoordinate::transformPath)
                                .map { path ->
                                    item.visibility = CACHED
                                    calculated++
                                    path.withMatrix(matrix, worldRotation)
                                        .also { item.addToRender(it) }
                                }
                                .also { item.cacheTranslated = it }
                        }
                    }
                    is PreparedCircleItem -> {
                        if (item.visibility == CACHED) {
                            item.addToRender(item.cacheTranslated!!)
                            skipped++
                        } else if (item.minZoom.isBiggerThanZoom()) {
                            item.point
                                .let(visibleGpsCoordinate::transformPoint)
                                .withMatrix(matrix, worldRotation)
                                .let { point ->
                                    item.visibility = CACHED
                                    calculated++
                                    item.addToRender(point)
                                    item.cacheTranslated = point
                                }
                        }
                    }
                    is PreparedIconItem -> {
                        if (item.visibility == CACHED) {
                            item.addToRender(item.cacheTranslated!!)
                            skipped++
                        } else if (item.minZoom.isBiggerThanZoom()) {
                            item.point
                                .let(visibleGpsCoordinate::transformPoint)
                                .withMatrix(matrix, worldRotation)
                                .let { point ->
                                    item.visibility = CACHED
                                    calculated++
                                    item.addToRender(point)
                                    item.cacheTranslated = point
                                }
                        }
                    }
                    is PreparedBitmapItem -> {
                        if (item.visibility == CACHED) {
                            item.addToRender(item.cacheTranslated!!)
                            skipped++
                        } else if (item.minZoom.isBiggerThanZoom()) {
                            item.point
                                .let(visibleGpsCoordinate::transformPoint)
                                .withMatrix(matrix, worldRotation)
                                .let { point ->
                                    item.visibility = CACHED
                                    calculated++
                                    item.addToRender(point)
                                    item.cacheTranslated = point
                                }
                        }
                    }
                }
            } else {
                hidden++
            }
        }
    }

    private fun Float?.isBiggerThanZoom(): Boolean =
        this == null || this > zoom

    private fun FloatArray.withMatrix(matrix: Matrix, worldRotation: Float): FloatArray {
        if (worldRotation != 0f) {
            matrix.mapPoints(this)
        }
        return this
    }

    private fun PreparedPolygonItem<T>.addToRender(
        array: FloatArray,
    ) {
        insides.add(
            RenderItem.RenderPolygonItem(
                array,
                paintHolder.takePaint(),
            )
        )
        if (outerPaintHolder != null) {
            borders.add(
                RenderItem.RenderPolygonItem(
                    array,
                    outerPaintHolder.takePaint(),
                )
            )
        }
    }

    private fun PreparedPathItem<T>.addToRender(
        array: FloatArray,
    ) {
        insides.add(
            RenderItem.RenderPathItem(
                array,
                paintHolder.takePaint(),
            )
        )
        if (outerPaintHolder != null) {
            borders.add(
                RenderItem.RenderPathItem(
                    array,
                    outerPaintHolder.takePaint(),
                )
            )
        }
    }

    private fun PreparedCircleItem<T>.addToRender(
        array: FloatArray,
    ) {
        insides.add(
            RenderItem.PointItem.RenderCircleItem(
                array[0],
                array[1],
                radius.takeDimension(),
                paintHolder.takePaint(),
            )
        )
        if (outerPaintHolder != null) {
            borders.add(
                RenderItem.PointItem.RenderCircleItem(
                    array[0],
                    array[1],
                    radius.takeDimension(),
                    outerPaintHolder.takePaint(),
                )
            )
        }
    }

    private fun PreparedIconItem<T>.addToRender(
        array: FloatArray,
    ) {
        icons.add(
            RenderItem.PointItem.RenderIconItem(
                array[0],
                array[1],
                icon,
                pivot,
            )
        )
    }

    private fun PreparedBitmapItem<T>.addToRender(
        array: FloatArray,
    ) {
        icons.add(
            RenderItem.PointItem.RenderBitmapItem(
                array[0],
                array[1],
                bitmap,
                pivot,
            )
        )
    }

    private fun MapDimension.takeDimension(): Float =
        dynamicDimensions[this]
            ?: bakeDimension(this).invoke(zoom, centerGpsCoordinate, currentWidth)
                .also { dynamicDimensions[this] = it }

    private fun PaintHolder<T>.takePaint(): T =
        when (this) {
            is PaintHolder.Static<T> -> {
                paint
            }
            is PaintHolder.Dynamic<T> -> {
                dynamicPaints[this]
                    ?: block(zoom, centerGpsCoordinate, currentWidth)
                        .also { dynamicPaints[this] = it }
            }
        }
}