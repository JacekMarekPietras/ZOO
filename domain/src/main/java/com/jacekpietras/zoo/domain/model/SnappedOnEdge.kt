package com.jacekpietras.zoo.domain.model

import com.jacekpietras.core.PointD
import com.jacekpietras.zoo.domain.business.Node

internal data class SnappedOnEdge(
    val point: PointD,
    val near1: Node,
    val near2: Node,
) {

    infix fun onSameEdge(right: SnappedOnEdge): Boolean =
        (this.near1 == right.near1 && this.near2 == right.near2) ||
                (this.near1 == right.near2 && this.near2 == right.near1)

    fun getUniqueNodes(): Set<Node> =
        when (point) {
            near1.point -> setOf(near1)
            near2.point -> setOf(near2)
            else -> setOf(near1, near2)
        }
}
