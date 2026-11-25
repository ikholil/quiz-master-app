package app.prostuti.core.common

import javax.inject.Qualifier

enum class ProstutiDispatcher { IO, DEFAULT }

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: ProstutiDispatcher)

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
