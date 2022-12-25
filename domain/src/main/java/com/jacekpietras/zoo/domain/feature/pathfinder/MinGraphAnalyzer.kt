package com.jacekpietras.zoo.domain.feature.pathfinder

import com.jacekpietras.geometry.PointD
import com.jacekpietras.zoo.domain.feature.pathfinder.model.MinEdge
import com.jacekpietras.zoo.domain.feature.pathfinder.model.MinNode
import com.jacekpietras.zoo.domain.feature.pathfinder.model.Node
import com.jacekpietras.zoo.domain.feature.pathfinder.model.SnappedOnMin
import kotlinx.coroutines.delay

internal class MinGraphAnalyzer {

    private var nodes: Collection<MinNode>? = null
    private val snapper = PointSnapper()

    fun initialize(fullNodes: Set<Node>) {
        val mappedNodes = fullNodes.map { MinNode(it.point) }.toMutableSet()
        fullNodes.forAllEdges { a, b, technical, weight ->
            val minA = mappedNodes.first { a.point == it.point }
            val minB = mappedNodes.first { b.point == it.point }
            minA.connect(minB, technical, weight, true)
            minB.connect(minA, technical, weight, false)
        }
        val (corners, minNodes) = mappedNodes.separateCorners()
        corners.forEach { corner ->
            corner.straightenCorner()
        }

        nodes = minNodes
    }

    suspend fun getShortestPath(
        endPoint: PointD,
        startPoint: PointD?,
        technicalAllowedAtStart: Boolean = true,
        technicalAllowedAtEnd: Boolean = false,
    ): List<PointD> {
        val nodes = waitForNodes()

        if (startPoint == null) return listOf(endPoint)
        if (nodes.isEmpty()) return listOf(endPoint)

        val snapStart = snapper.getSnappedOnMinEdge(nodes, startPoint, technicalAllowed = technicalAllowedAtStart)
        val snapEnd = nodes.first { it.point == endPoint }

        val result = getShortestPathJob(
            start = snapStart,
            end = snapEnd,
            technicalAllowed = technicalAllowedAtEnd,
        )
        return result.pointPath()
    }

    private suspend fun getShortestPathJob(
        start: SnappedOnMin,
        end: MinNode,
        technicalAllowed: Boolean = false,
    ): List<MinNode> =
        MinDijkstra(
            vertices = waitForNodes(),
            technicalAllowed = technicalAllowed,
        ).calculate(
            start = start,
            end = end,
        )

    internal suspend fun waitForNodes(): Collection<MinNode> {
        while (nodes == null) {
            delay(100)
        }
        return checkNotNull(nodes)
    }

    private fun Collection<MinNode>.separateCorners() =
        partition { node ->
            val edges = node.edges.distinctBy { it.node.point }
            edges.size == 2 && edges.first().technical == edges.last().technical
        }

    private fun MinNode.straightenCorner() {
        val e1 = edges.first()
        val e2 = edges.last()
        val from = e1.node
        val to = e2.node
        val weight = e1.weight + e2.weight
        val corners = e1.corners.reversed().map { (p, weight) -> p to e1.weight - weight } +
                (point to e1.weight) +
                e2.corners.map { (p, weight) -> p to e1.weight + weight }

        from.edges.add(MinEdge(to, from, e1.technical, weight, true, corners))
        to.edges.add(MinEdge(from, to, e1.technical, weight, false, corners.reversed()))

        from.edges.remove(from.edges.first { it.node == this })
        to.edges.remove(to.edges.first { it.node == this })
    }

    private fun List<MinNode>.pointPath() =
        zipWithNext { a, b ->
            listOf(a.point) + a.edges.find { it.node == b }.cornerPoints()
        }.flatten() + last().point

    private fun MinEdge?.cornerPoints() =
        this?.corners?.map(Pair<PointD, Double>::first) ?: emptyList()
}
