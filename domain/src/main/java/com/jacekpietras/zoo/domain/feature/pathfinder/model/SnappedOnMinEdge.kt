package com.jacekpietras.zoo.domain.feature.pathfinder.model

import com.jacekpietras.geometry.PointD

internal sealed class SnappedOnMin {

    internal data class SnappedOnMinEdge(
        val point: PointD,
        val edge: MinEdge,
        val weightFromStart: Double,
    ) : SnappedOnMin() {

        fun reversed() =
            SnappedOnMinEdge(
                point = point,
                edge = edge.reversed(),
                weightFromStart = edge.weight - weightFromStart,
            )
    }

    internal data class SnappedOnMinNode(
        val node: MinNode,
    ) : SnappedOnMin()
}
