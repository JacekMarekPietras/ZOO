package com.jacekpietras.zoo.domain.feature.tsp

internal class DivorcedTSPAlgorithm<T : Any>(
    private val algorithm: TSPAlgorithm<T>,
    private val dummy: T, // todo think how to create new dummy inside
) : TSPWithFixedStagesAlgorithm<T> {

    override suspend fun run(
        points: List<T>,
        distanceCalculation: suspend (T, T) -> Double,
        immutablePositions: List<Int>?
    ): Pair<Double, List<T>> {
        immutablePositions?.verifyMutablePositionsInMiddle()

        val firstIsFixed = immutablePositions?.contains(0) == true
        val lastIsFixed = immutablePositions?.contains(points.lastIndex) == true

        val first = if (firstIsFixed) points.first() else null
        val last = if (lastIsFixed) points.last() else null

        val distanceCalculationWithoutFixed: suspend (T, T) -> Double = { a, b ->
            when {
                a === dummy -> 0.0
                b === dummy -> 0.0
                else -> distanceCalculation(a, b)
            }
        }

        //       r        0         0         r
        //  sth <-> last <-> dummy <-> first <-> sth
        //           sth <->       <-> sth
        //               max       max
        val distanceCalculationWithFixed: suspend (T, T) -> Double = { a, b ->
            when {
                a === dummy && b === first -> 0.0
                a === dummy && b === last -> 0.0
                a === first && b === dummy -> 0.0
                a === last && b === dummy -> 0.0
                a === first && b === last -> Double.MAX_VALUE
                a === last && b === first -> Double.MAX_VALUE
                a === dummy -> Double.MAX_VALUE
                b === dummy -> Double.MAX_VALUE
                else -> distanceCalculation(a, b)
            }
        }

        val composedDistanceCalculation = if (firstIsFixed || lastIsFixed) {
            distanceCalculationWithFixed
        } else {
            distanceCalculationWithoutFixed
        }

        val tour = algorithm
            .run(
                points = points + dummy,
                distanceCalculation = composedDistanceCalculation,
            )
            .second

        var connected = tour.shiftStartTo()
        if (connected.first() === last || connected.last() === first) {
            connected = connected.reversed()
        }
        if (first != null && first !== connected.first()) {
            throw IllegalStateException("Fixed first element is not at the beginning")
        }
        if (last != null && last !== connected.last()) {
            throw IllegalStateException("Fixed last element is not at the end")
        }

        val distance = connected.zipWithNext { a, b -> distanceCalculation(a, b) }.sum()

        return distance to connected
    }


    private fun List<T>.shiftStartTo(): List<T> {
        val indexOfDummy = indexOfFirst { it == dummy }
        val begin = subList(0, indexOfDummy)
        val end = subList(indexOfDummy + 1, lastIndex)
        return end + begin
    }

    private fun List<Int>.verifyMutablePositionsInMiddle() {
        val immutablePointsInMiddle = any { it != 0 && it != lastIndex }
        if (immutablePointsInMiddle) {
            throw IllegalArgumentException("Supported only immutable positions on begin and end of list")
        }
    }

    override suspend fun run(points: List<T>, distanceCalculation: suspend (T, T) -> Double): Pair<Double, List<T>> =
        run(points, distanceCalculation, null)
}
