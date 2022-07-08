package com.jacekpietras.zoo.catalogue.feature.animal.viewmodel

import androidx.lifecycle.ViewModel
import com.jacekpietras.geometry.PointD
import com.jacekpietras.zoo.catalogue.feature.animal.mapper.AnimalMapper
import com.jacekpietras.zoo.catalogue.feature.animal.model.AnimalState
import com.jacekpietras.zoo.catalogue.feature.animal.model.AnimalViewState
import com.jacekpietras.zoo.catalogue.feature.animal.router.AnimalRouter
import com.jacekpietras.zoo.catalogue.utils.combine
import com.jacekpietras.zoo.catalogue.utils.reduceOnMain
import com.jacekpietras.zoo.core.dispatcher.dispatcherProvider
import com.jacekpietras.zoo.core.dispatcher.launchInBackground
import com.jacekpietras.zoo.domain.feature.animal.interactor.GetAnimalPositionUseCase
import com.jacekpietras.zoo.domain.feature.animal.interactor.GetAnimalUseCase
import com.jacekpietras.zoo.domain.feature.animal.interactor.IsAnimalSeenUseCase
import com.jacekpietras.zoo.domain.feature.favorites.interactor.ObserveAnimalFavoritesUseCase
import com.jacekpietras.zoo.domain.feature.favorites.interactor.SetAnimalFavoriteUseCase
import com.jacekpietras.zoo.domain.feature.map.interactor.ObserveAviaryUseCase
import com.jacekpietras.zoo.domain.feature.map.interactor.ObserveBuildingsUseCase
import com.jacekpietras.zoo.domain.feature.map.interactor.ObserveRoadsUseCase
import com.jacekpietras.zoo.domain.feature.map.interactor.ObserveWorldBoundsUseCase
import com.jacekpietras.zoo.domain.feature.map.model.MapItemEntity
import com.jacekpietras.zoo.domain.feature.pathfinder.interactor.GetShortestPathUseCase
import com.jacekpietras.zoo.domain.feature.planner.interactor.AddAnimalToCurrentPlanUseCase
import com.jacekpietras.zoo.domain.feature.planner.interactor.RemoveAnimalFromCurrentPlanUseCase
import com.jacekpietras.zoo.domain.interactor.ObserveUserPositionUseCase
import com.jacekpietras.zoo.domain.model.AnimalId
import com.jacekpietras.zoo.domain.model.RegionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class AnimalViewModel(
    animalId: AnimalId,
    mapper: AnimalMapper = AnimalMapper(),
    getAnimalUseCase: GetAnimalUseCase,
    isAnimalSeenUseCase: IsAnimalSeenUseCase,
    observeAnimalFavoritesUseCase: ObserveAnimalFavoritesUseCase,
    private val setAnimalFavoriteUseCase: SetAnimalFavoriteUseCase,
    private val addAnimalToCurrentPlanUseCase: AddAnimalToCurrentPlanUseCase,
    private val removeFromCurrentPlanUseCase: RemoveAnimalFromCurrentPlanUseCase,
    observeWorldBoundsUseCase: ObserveWorldBoundsUseCase,
    observeBuildingsUseCase: ObserveBuildingsUseCase,
    observeAviaryUseCase: ObserveAviaryUseCase,
    observeRoadsUseCase: ObserveRoadsUseCase,
    observeUserPositionUseCase: ObserveUserPositionUseCase,
    getAnimalPositionUseCase: GetAnimalPositionUseCase,
    private val getShortestPathUseCase: GetShortestPathUseCase,
) : ViewModel() {

    private val state = MutableStateFlow(AnimalState(animalId = animalId))
    private val currentState get() = state.value
    val viewState: Flow<AnimalViewState?> = combine(
        observeWorldBoundsUseCase.run().flowOn(dispatcherProvider.default),
        observeBuildingsUseCase.run().flowOn(dispatcherProvider.default),
        observeAviaryUseCase.run().flowOn(dispatcherProvider.default),
        observeRoadsUseCase.run().flowOn(dispatcherProvider.default),
        observeUserPositionUseCase.run().map(this::getPaths).flowOn(dispatcherProvider.default),
        state,
        mapper::from,
    )

    init {
        launchInBackground {
            val animal = getAnimalUseCase.run(animalId)
            state.reduceOnMain { copy(animal = animal) }

            observeAnimalFavoritesUseCase.run()
                .onEach { favorites ->
                    val isFavorite = favorites.contains(animalId)
                    state.reduceOnMain {
                        copy(isFavorite = isFavorite)
                    }
                }
                .launchIn(this)

            val isSeen = isAnimalSeenUseCase.run(animalId)
            val positions = getAnimalPositionUseCase.run(animalId)

            state.reduceOnMain {
                copy(
                    isSeen = isSeen,
                    animalPositions = positions,
                )
            }
        }
    }

    private suspend fun getPaths(
        position: PointD
    ): List<MapItemEntity.PathEntity> {
        return currentState.animalPositions
            .map { getShortestPathUseCase.run(position, it) }
            .map { MapItemEntity.PathEntity(it) }
    }

    fun onWikiClicked(router: AnimalRouter) {
        router.navigateToWiki(checkNotNull(currentState.animal).wiki)
    }

    fun onWebClicked(router: AnimalRouter) {
        router.navigateToWeb(checkNotNull(currentState.animal).web)
    }

    fun onNavClicked(router: AnimalRouter, regionId: RegionId? = null) {
        router.navigateToMap(checkNotNull(currentState.animal).id, regionId)
    }

    fun onFavoriteClicked() {
        val isFavorite = (currentState.isFavorite ?: false).not()
        val animalId = currentState.animalId

        launchInBackground {
            state.reduceOnMain { copy(isFavorite = isFavorite) }
            setAnimalFavoriteUseCase.run(
                animalId = animalId,
                isFavorite = isFavorite,
            )
            if (isFavorite) {
                addAnimalToCurrentPlanUseCase.run(animalId)
            } else {
                removeFromCurrentPlanUseCase.run(animalId)
            }
        }
    }
}
