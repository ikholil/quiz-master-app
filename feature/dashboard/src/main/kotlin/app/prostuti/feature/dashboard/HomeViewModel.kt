package app.prostuti.feature.dashboard

import androidx.lifecycle.viewModelScope
import app.prostuti.core.common.AppResult
import app.prostuti.core.domain.repository.DashboardRepository
import app.prostuti.core.domain.repository.PlannerRepository
import app.prostuti.core.model.Dashboard
import app.prostuti.core.model.PlanTaskStatus
import app.prostuti.core.ui.LoadViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dashboard: DashboardRepository,
    private val planner: PlannerRepository,
) : LoadViewModel<Dashboard>() {
    init { load() }

    override suspend fun fetch(): AppResult<Dashboard> = dashboard.dashboard()

    /** Optimistic toggle; reverts and reports if the server rejects it. */
    fun toggleTask(taskId: String) {
        var previous: PlanTaskStatus? = null
        update { d ->
            d.copy(todayTasks = d.todayTasks.map {
                if (it.id == taskId) { previous = it.status; it.copy(status = if (it.status == PlanTaskStatus.DONE) PlanTaskStatus.PENDING else PlanTaskStatus.DONE) } else it
            })
        }
        val next = if (previous == PlanTaskStatus.DONE) PlanTaskStatus.PENDING else PlanTaskStatus.DONE
        viewModelScope.launch {
            val r = planner.setTaskStatus(taskId, next)
            if (r is AppResult.Failure) {
                update { d -> d.copy(todayTasks = d.todayTasks.map { if (it.id == taskId) it.copy(status = previous ?: it.status) else it }) }
                report(r.error)
            }
        }
    }
}
