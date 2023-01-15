package com.jacekpietras.zoo.scrapper.di

import com.jacekpietras.zoo.data.parser.RegionIdAdapter
import com.jacekpietras.zoo.scrapper.data.WebScrapper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.koin.dsl.module

val scrapperModule = module {

    single<Moshi> {
        Moshi.Builder()
            .add(RegionIdAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    single {
        WebScrapper(
            moshi = get(),
            animalRepository = get(),
        )
    }
}
