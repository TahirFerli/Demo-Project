package com.example.demo.di

import com.example.demo.data.repository.PhotoRepositoryImpl
import com.example.demo.domain.repository.PhotoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** @Binds (abstract) is cheaper than @Provides: no extra factory method is generated. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository
}
