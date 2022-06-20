package com.jacekpietras.zoo.map.model

import com.jacekpietras.core.PointD
import com.jacekpietras.zoo.domain.feature.map.model.MapItemEntity
import com.jacekpietras.zoo.domain.model.ThemeType

internal data class MapVolatileState(
    val compass: Float = 0f,
    val userPosition: PointD = PointD(),
    val snappedPoint: PointD? = null,
    val takenRoute: List<MapItemEntity.PathEntity> = emptyList(),
    val visitedRoads: List<MapItemEntity.PathEntity> = emptyList(),
    val shortestPath: List<PointD> = emptyList(),
    val plannedPath: List<PointD> = emptyList(),
)
