package com.jacekpietras.zoo.planner.model

internal data class PlannerViewState(
    val list: List<PlannerItem> = emptyList(),
    val isEmptyViewVisible :Boolean = false,
    val isAddExitVisible :Boolean = false,
)
