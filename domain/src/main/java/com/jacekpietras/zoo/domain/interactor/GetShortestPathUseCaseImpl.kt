package com.jacekpietras.zoo.domain.interactor

import com.jacekpietras.core.PointD
import com.jacekpietras.zoo.domain.business.GraphAnalyzer

internal class GetShortestPathUseCaseImpl(
    private val initializeGraphAnalyzerIfNeededUseCase: InitializeGraphAnalyzerIfNeededUseCase,
    private val graphAnalyzer: GraphAnalyzer,
) : GetShortestPathUseCase {

    override suspend fun run(start: PointD, end: PointD): List<PointD> {
        initializeGraphAnalyzerIfNeededUseCase.run()
        return graphAnalyzer.getShortestPath(
            endPoint = end,
            startPoint = start,
        )
    }
}