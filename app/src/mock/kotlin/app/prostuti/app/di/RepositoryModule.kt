package app.prostuti.app.di

import app.prostuti.core.data.repository.LiveSettingsRepository
import app.prostuti.core.domain.repository.*
import app.prostuti.core.mockbackend.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * DEVELOPMENT ONLY — bindings for the `mock` flavor. Remote repositories are replaced by
 * in-memory sample implementations; device settings still use the real DataStore.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun auth(impl: MockAuthRepository): AuthRepository
    @Binds abstract fun profile(impl: MockProfileRepository): ProfileRepository
    @Binds abstract fun settings(impl: LiveSettingsRepository): SettingsRepository
    @Binds abstract fun dashboard(impl: MockDashboardRepository): DashboardRepository
    @Binds abstract fun exams(impl: MockExamDirectoryRepository): ExamRepository
    @Binds abstract fun learning(impl: MockLearningRepository): LearningRepository
    @Binds abstract fun questions(impl: MockQuestionRepository): QuestionRepository
    @Binds abstract fun practice(impl: MockPracticeRepository): PracticeRepository
    @Binds abstract fun mocks(impl: MockMockExamRepository): MockExamRepository
    @Binds abstract fun analytics(impl: MockAnalyticsRepository): AnalyticsRepository
    @Binds abstract fun tutor(impl: MockTutorRepository): TutorRepository
    @Binds abstract fun planner(impl: MockPlannerRepository): PlannerRepository
    @Binds abstract fun revision(impl: MockRevisionRepository): RevisionRepository
    @Binds abstract fun community(impl: MockCommunityRepository): CommunityRepository
    @Binds abstract fun account(impl: MockAccountRepository): AccountRepository
    @Binds abstract fun offline(impl: MockOfflineRepository): OfflineRepository
}
