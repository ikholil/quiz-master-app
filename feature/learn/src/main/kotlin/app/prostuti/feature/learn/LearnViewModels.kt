package app.prostuti.feature.learn

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.prostuti.core.common.AppError
import app.prostuti.core.common.AppResult
import app.prostuti.core.common.getOrNull
import app.prostuti.core.domain.repository.LearningRepository
import app.prostuti.core.domain.repository.OfflineRepository
import app.prostuti.core.domain.repository.ProfileRepository
import app.prostuti.core.model.*
import app.prostuti.core.ui.LoadViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyllabusOverview(val examId: String?, val targets: List<TargetExam>, val subjects: List<Subject>)

@HiltViewModel
class LearnViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val learning: LearningRepository,
    private val profiles: ProfileRepository,
) : LoadViewModel<SyllabusOverview>() {
    private var examId: String? = savedStateHandle.toRoute<LearnRoute>().examId
    init { load() }

    override suspend fun fetch(): AppResult<SyllabusOverview> {
        val profile = profiles.profile.first() ?: profiles.refresh().getOrNull()
        val targets = profile?.targets.orEmpty()
        val id = examId ?: profile?.primaryTarget?.examId ?: return AppResult.Success(SyllabusOverview(null, targets, emptyList()))
        return when (val r = learning.subjects(id)) {
            is AppResult.Success -> AppResult.Success(SyllabusOverview(id, targets, r.data))
            is AppResult.Failure -> r
        }
    }

    fun selectExam(id: String) { examId = id; load() }
}

@HiltViewModel
class SubjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val learning: LearningRepository,
) : LoadViewModel<List<Chapter>>() {
    val route = savedStateHandle.toRoute<SubjectRoute>()
    init { load() }
    override suspend fun fetch() = learning.chapters(route.subjectId)
}

@HiltViewModel
class TopicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val learning: LearningRepository,
    private val offline: OfflineRepository,
) : LoadViewModel<TopicDetail>() {
    val topicId = savedStateHandle.toRoute<TopicRoute>().topicId
    init { load() }
    override suspend fun fetch() = learning.topic(topicId)

    fun toggleComplete() {
        var completed = false
        update { d -> completed = !d.summary.isCompleted; d.copy(summary = d.summary.copy(isCompleted = completed)) }
        launchAction { learning.setTopicCompleted(topicId, completed) }
    }

    fun toggleBookmark() {
        var b = false
        update { d -> b = !d.isBookmarked; d.copy(isBookmarked = b) }
        launchAction { learning.setTopicBookmarked(topicId, b) }
    }

    fun saveNote(note: String, onSaved: () -> Unit) = viewModelScope.launch {
        when (val r = learning.saveTopicNote(topicId, note)) {
            is AppResult.Success -> { update { it.copy(note = note) }; onSaved() }
            is AppResult.Failure -> report(r.error)
        }
    }

    fun download(onStarted: () -> Unit) = viewModelScope.launch {
        when (val r = offline.downloadTopic(topicId)) {
            is AppResult.Success -> { onStarted(); load() }
            is AppResult.Failure -> report(r.error)
        }
    }
}

@HiltViewModel
class MaterialViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val learning: LearningRepository,
) : LoadViewModel<LearningMaterial>() {
    private val materialId = savedStateHandle.toRoute<MaterialRoute>().materialId
    init { load() }
    override suspend fun fetch(): AppResult<LearningMaterial> = learning.material(materialId).let {
        if (it is AppResult.Failure && it.error == AppError.NotFound) it else it
    }
}
