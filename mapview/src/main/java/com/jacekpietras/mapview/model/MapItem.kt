package com.jacekpietras.mapview.model

import com.jacekpietras.core.PointD

class MapItem {
    val shape: Any
    val paint: MapPaint

    constructor(
        polygon: PolygonD,
        paint: MapPaint,
    ) {
        this.shape = polygon
        this.paint = paint
    }

    constructor(
        path: PathD,
        paint: MapPaint,
    ) {
        this.shape = path
        this.paint = paint
    }

    constructor(
        point: PointD,
        paint: MapPaint,
    ) {
        this.shape = point
        this.paint = paint
    }
}
