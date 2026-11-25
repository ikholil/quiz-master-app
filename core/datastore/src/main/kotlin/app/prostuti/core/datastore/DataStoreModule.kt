package app.prostuti.core.datastore

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {
    @Binds abstract fun tokenStore(impl: KeystoreTokenStore): TokenStore
}
