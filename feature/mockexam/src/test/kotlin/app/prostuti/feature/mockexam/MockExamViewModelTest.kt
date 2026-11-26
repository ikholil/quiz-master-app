package app.prostuti.feature.mockexam

import androidx.lifecycle.SavedStateHandle
import app.prostuti.core.domain.exam.QuestionStatus
import app.prostuti.core.domain.usecase.StartMockExamUseCase
import app.prostuti.core.domain.usecase.SubmitMockExamUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MockExamViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val time = FakeTime()
    private lateinit var repo: FakeMockRepository
    private lateinit var handle: SavedStateHandle

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeMockRepository()
        handle = SavedStateHandle(mapOf("mockId" to "m1"))
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): MockExamViewModel =
        MockExamViewModel(handle, StartMockExamUseCase(repo), repo, SubmitMockExamUseCase(repo), time).also { runCurrent() }

    @Test fun `starts a server attempt and remembers it for process death`() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(ExamPhase.Running, vm.state.value.phase)
        assertEquals("att-1", handle.get<String>(MockExamViewModel.KEY_ATTEMPT))
        assertEquals(Duration.ofMinutes(10), vm.state.value.remaining)
        vm.submit(); runCurrent()
    }

    @Test fun `recreated view model resumes the same attempt instead of starting a new one`() = runTest(dispatcher) {
        handle[MockExamViewModel.KEY_ATTEMPT] = "att-1"
        val vm = viewModel()
        assertEquals(1, repo.resumeCalls)
        assertEquals(0, repo.startCalls)
        vm.submit(); runCurrent()
    }

    @Test fun `questions carry no solutions during the exam`() = runTest(dispatcher) {
        val vm = viewModel()
        assertTrue(vm.state.value.session!!.allQuestions.all { it.solution == null })
        vm.submit(); runCurrent()
    }

    @Test fun `navigation, selection and review marks update the answer sheet`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.select("q1-b")
        vm.next()
        vm.toggleReview()
        vm.next()
        val s = vm.state.value.session!!
        assertEquals("q3", s.currentQuestion.id)
        assertEquals(QuestionStatus.ANSWERED, s.statusOf("q1"))
        assertEquals(QuestionStatus.MARKED_FOR_REVIEW, s.statusOf("q2"))
        assertEquals(QuestionStatus.NOT_ANSWERED, s.statusOf("q3"))
        assertEquals(QuestionStatus.NOT_VISITED, s.statusOf("q4"))
        vm.submit(); runCurrent()
    }

    @Test fun `dirty answers are autosaved`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.select("q1-c")
        advanceTimeBy(MockExamViewModel.AUTOSAVE_MS + 100)
        runCurrent()
        assertEquals(mapOf("q1" to setOf("q1-c")), repo.saves.last())
        assertEquals(SaveStatus.SAVED, vm.state.value.saveStatus)
        assertEquals(false, vm.state.value.session!!.dirty)
        vm.submit(); runCurrent()
    }

    @Test fun `changing the device wall clock does not extend the exam`() = runTest(dispatcher) {
        val vm = viewModel()
        time.wall = time.wall.minus(Duration.ofDays(2))
        time.monotonic += 4 * 60_000
        advanceTimeBy(1_100); runCurrent()
        assertEquals(Duration.ofMinutes(6), vm.state.value.remaining)
        vm.submit(); runCurrent()
    }

    @Test fun `exam auto-submits when server time runs out`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.select("q1-a")
        time.monotonic += 10 * 60_000
        advanceTimeBy(1_100); runCurrent()
        assertIs<ExamPhase.Submitted>(vm.state.value.phase)
        assertEquals(mapOf("q1" to setOf("q1-a")), repo.submissions.single())
    }

    @Test fun `submission retries transient failures and answers cannot change afterwards`() = runTest(dispatcher) {
        repo.submitFailuresRemaining = 1
        val vm = viewModel()
        vm.select("q1-d")
        vm.submit()
        advanceTimeBy(1_500); runCurrent()
        assertIs<ExamPhase.Submitted>(vm.state.value.phase)
        vm.select("q1-a")
        assertEquals(setOf("q1-d"), vm.state.value.session!!.answers["q1"])
        assertEquals(1, repo.submissions.size)
    }

    @Test fun `failed submission keeps answers and allows retry`() = runTest(dispatcher) {
        repo.submitFailuresRemaining = 5
        val vm = viewModel()
        vm.select("q2-b".replace("q2", "q1"))
        vm.submit()
        advanceTimeBy(10_000); runCurrent()
        assertIs<ExamPhase.SubmitFailed>(vm.state.value.phase)
        repo.submitFailuresRemaining = 0
        vm.submit(); advanceTimeBy(100); runCurrent()
        assertIs<ExamPhase.Submitted>(vm.state.value.phase)
        assertNull(repo.submissions.single()["q2"])
    }
}
