package app.prostuti.app.di

import app.prostuti.core.data.repository.*
import app.prostuti.core.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Production bindings: every repository talks to the NestJS backend (with local caching/offline). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun auth(impl: LiveAuthRepository): AuthRepository
    @Binds abstract fun profile(impl: LiveProfileRepository): ProfileRepository
    @Binds abstract fun settings(impl: LiveSettingsRepository): SettingsRepository
    @Binds abstract fun dashboard(impl: LiveDashboardRepository): DashboardRepository
    @Binds abstract fun exams(impl: LiveExamRepository): ExamRepository
    @Binds abstract fun learning(impl: LiveLearningRepository): LearningRepository
    @Binds abstract fun questions(impl: LiveQuestionRepository): QuestionRepository
    @Binds abstract fun practice(impl: LivePracticeRepository): PracticeRepository
    @Binds abstract fun mocks(impl: LiveMockExamRepository): MockExamRepository
    @Binds abstract fun analytics(impl: LiveAnalyticsRepository): AnalyticsRepository
    @Binds abstract fun tutor(impl: LiveTutorRepository): TutorRepository
    @Binds abstract fun planner(impl: LivePlannerRepository): PlannerRepository
    @Binds abstract fun revision(impl: LiveRevisionRepository): RevisionRepository
    @Binds abstract fun community(impl: LiveCommunityRepository): CommunityRepository
    @Binds abstract fun account(impl: LiveAccountRepository): AccountRepository
    @Binds abstract fun offline(impl: LiveOfflineRepository): OfflineRepository
}
