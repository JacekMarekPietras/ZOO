package com.jacekpietras.zoo.map.model

import com.jacekpietras.zoo.domain.model.LatLon
import com.jacekpietras.zoo.map.ui.MapItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class MapViewState(
    val mapData: Flow<List<MapItem>> = MutableStateFlow(emptyList()),
    val userPosition: Flow<LatLon> = MutableStateFlow(LatLon(0.0, 0.0)),
)