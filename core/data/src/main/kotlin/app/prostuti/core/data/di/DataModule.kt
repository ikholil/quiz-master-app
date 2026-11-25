package app.prostuti.core.data.di

import android.content.Context
import app.prostuti.core.common.ApplicationScope
import app.prostuti.core.common.Dispatcher
import app.prostuti.core.common.ProstutiDispatcher
import app.prostuti.core.common.SystemTimeSource
import app.prostuti.core.common.TimeSource
import app.prostuti.core.data.util.ConnectivityNetworkMonitor
import app.prostuti.core.domain.repository.NetworkMonitor
import android.os.SystemClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

/** Infrastructure shared by every flavor. Repository bindings live in :app (live vs mock flavor). */
@Module
@InstallIn(SingletonComponent::class)
object InfrastructureModule {
    @Provides @Dispatcher(ProstutiDispatcher.IO) fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
    @Provides @Dispatcher(ProstutiDispatcher.DEFAULT) fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides @Singleton @ApplicationScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun timeSource(): TimeSource = SystemTimeSource(monotonic = { SystemClock.elapsedRealtime() })

    @Provides @Named("cacheDir")
    fun cacheDir(@ApplicationContext context: Context): File = context.cacheDir
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorModule {
    @Binds abstract fun networkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}
