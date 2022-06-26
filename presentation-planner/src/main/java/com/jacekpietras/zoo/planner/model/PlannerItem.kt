package com.jacekpietras.zoo.planner.model

import com.jacekpietras.zoo.core.text.Text

data class PlannerItem(
    val text: Text,
    val regionId: String,
    val isMultiple: Boolean = false,
    val isMutable: Boolean = false,
    val isFixed: Boolean = false,
)
