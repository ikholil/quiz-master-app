package app.prostuti.feature.mockexam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.prostuti.core.designsystem.theme.ProstutiTheme
import app.prostuti.core.domain.exam.ExamSessionState
import app.prostuti.core.model.AppLanguage
import app.prostuti.core.ui.LocalAppLanguage
import androidx.compose.runtime.CompositionLocalProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.test.assertEquals

/** UI test for the exam screen, run on the JVM with Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en")
class MockExamContentTest {
    @get:Rule val compose = createComposeRule()

    private fun running(): MockExamUiState {
        val a = attempt()
        return MockExamUiState(ExamPhase.Running, a, ExamSessionState(a.sections).visitCurrent(), Duration.ofMinutes(9).plusSeconds(5))
    }

    @Test fun selectingAnOptionNavigatingAndSubmitting() {
        var state by mutableStateOf(running())
        var submitted = false
        compose.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) { ProstutiTheme(darkTheme = false) {
                MockExamContent(
                    state = state,
                    onSelect = { state = state.copy(session = state.session!!.select(it)) },
                    onClear = {}, onReview = { state = state.copy(session = state.session!!.toggleMarkForReview()) },
                    onNext = { state = state.copy(session = state.session!!.next()) },
                    onPrevious = {}, onGoTo = { state = state.copy(session = state.session!!.goTo(it)) },
                    onSection = {}, onSubmit = { submitted = true }, onRetryStart = {}, onLeave = {},
                )
            } }
        }
        compose.onNodeWithText("Question q1").assertIsDisplayed()
        compose.onNodeWithText("09:05").assertIsDisplayed()
        compose.onNodeWithText("Option b").performClick()
        compose.onNodeWithText("Option b").assertIsSelected()
        // No answer feedback during the exam.
        compose.onNodeWithText("Explanation").assertDoesNotExist()

        compose.onNodeWithTag("next").performClick()
        compose.onNodeWithText("Question q2").assertIsDisplayed()
        compose.onNodeWithTag("markReview").performClick()
        assertEquals(setOf("q2"), state.session!!.markedForReview)

        repeat(2) { compose.onNodeWithTag("next").performClick() }
        compose.onNodeWithTag("submit").performClick()
        compose.onNodeWithText("Answered 1 · Not answered 3 · Marked 1").assertIsDisplayed()
        compose.onNodeWithTag("confirmSubmit").performClick()
        assertEquals(true, submitted)
    }

    @Test fun negativeMarkingIsShownForTheSection() {
        compose.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) { ProstutiTheme(darkTheme = false) {
                MockExamContent(running(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
            } }
        }
        compose.onNodeWithText("−0.5 for each wrong answer · skipping costs nothing").performScrollTo().assertIsDisplayed()
    }
}
