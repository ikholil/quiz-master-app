package app.prostuti.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * End-to-end flow on the JVM against the development mock backend:
 * sign in with OTP → onboarding → home → practice → model test submission → result.
 * Screenshots are written to build/screenshots for visual review.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [34], qualifiers = "bn-w411dp-h891dp-xxhdpi")
class EndToEndFlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    private val outDir = File(System.getProperty("screenshot.dir") ?: "build/screenshots").apply { mkdirs() }

    @Before fun inject() = hilt.inject()

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.activityRule.scenario.onActivity { activity ->
            val view = activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun waitFor(text: String, timeout: Long = 10_000) {
        try {
            // Robolectric's main looper clock only moves when told to; advance it so coroutine delays elapse.
            var waited = 0L
            while (compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isEmpty()) {
                if (waited > timeout) throw AssertionError("not found")
                org.robolectric.shadows.ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                compose.waitForIdle()
                waited += 100
            }
        } catch (e: Throwable) {
            runCatching { shot("FAILED_waiting_for") }
            runCatching { compose.onRoot().printToLog("E2E") }
            throw AssertionError("Timed out waiting for '$text'", e)
        }
    }

    private fun click(text: String): SemanticsNodeInteraction =
        compose.onAllNodesWithText(text, substring = true).onFirst().apply { performScrollTo(); performClick() }

    private fun tab(label: String) = compose.onAllNodes(hasText(label, substring = false)).onFirst().performClick()

    private fun clickNoScroll(text: String) = compose.onAllNodesWithText(text, substring = true).onFirst().performClick()

    @Test fun signInOnboardPracticeAndTakeAMockExam() {
        // Welcome
        waitFor("মোবাইল নম্বর দিয়ে শুরু করুন")
        shot("01_welcome")
        clickNoScroll("মোবাইল নম্বর দিয়ে শুরু করুন")

        // Phone → OTP (development OTP 123456)
        waitFor("আপনার মোবাইল নম্বর কত?")
        compose.onNode(hasSetTextAction()).performTextInput("01712345678")
        shot("02_phone")
        clickNoScroll("কোড পাঠান")
        waitFor("কোডটি লিখুন")
        compose.onNode(hasSetTextAction()).performTextInput("123456")
        shot("03_otp")

        // Onboarding
        waitFor("চলুন আপনার প্রস্তুতি সাজাই")
        compose.onNode(hasSetTextAction()).performTextInput("রাহিম উদ্দিন")
        shot("04_onboarding_about")
        clickNoScroll("এগিয়ে যান")
        waitFor("বাংলাদেশ সিভিল সার্ভিস")
        click("বাংলাদেশ সিভিল সার্ভিস")
        click("এনটিআরসিএ শিক্ষক নিবন্ধন")
        shot("05_onboarding_exams")
        clickNoScroll("এগিয়ে যান")
        waitFor("পরীক্ষা কবে?")
        shot("06_onboarding_date")
        clickNoScroll("এগিয়ে যান")
        waitFor("প্রতিদিন কতক্ষণ পড়তে পারবেন?")
        click("নিয়মিত")
        clickNoScroll("এগিয়ে যান")
        waitFor("আপনি কোথা থেকে শুরু করছেন?")
        click("কিছুটা প্রস্তুতি আছে")
        shot("07_onboarding_level")
        clickNoScroll("এগিয়ে যান")
        waitFor("শক্তিশালী ও দুর্বল বিষয়")
        waitFor("গাণিতিক যুক্তি")
        shot("08_onboarding_subjects")
        clickNoScroll("এগিয়ে যান")
        waitFor("আপনার প্রথম তিন দিন")
        waitFor("মিনিট", 15_000)
        shot("09_onboarding_plan")
        clickNoScroll("প্রস্তুতি শুরু করুন")

        // Home dashboard
        waitFor("আজকের পরিকল্পনা", 15_000)
        shot("10_home")

        // Learn tab
        tab("পড়ুন")
        waitFor("বাংলাদেশ বিষয়াবলি")
        shot("11_learn")
        click("বাংলাদেশ বিষয়াবলি")
        waitFor("১৯৭১ সালের মুক্তিযুদ্ধ")
        click("১৯৭১ সালের মুক্তিযুদ্ধ")
        waitFor("পাঠ্য উপকরণ")
        shot("12_topic")
        click("স্টাডি নোট")
        waitFor("গুরুত্বপূর্ণ তারিখ")
        shot("13_material")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor("এই টপিক অনুশীলন করুন")
        click("এই টপিক অনুশীলন করুন")

        // Practice with instant feedback
        waitFor("বাদ দিন", 15_000)
        shot("14_practice_question")
        tab("ক") // exact match: the option letter
        waitFor("ব্যাখ্যা")
        shot("15_practice_feedback")

        // Model test from the Practice tab
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor("ছেড়ে যান")
        clickNoScroll("ছেড়ে যান")
        waitFor("এই টপিক অনুশীলন করুন")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        tab("অনুশীলন")
        waitFor("আজকের চ্যালেঞ্জ")
        shot("16_practice_hub")
        click("মডেল টেস্ট")
        waitFor("বিসিএস প্রিলিমিনারি মডেল টেস্ট ০১")
        shot("17_mock_list")
        click("বিসিএস প্রিলিমিনারি মডেল টেস্ট ০১")
        waitFor("শুরু করার আগে")
        shot("18_mock_instructions")
        clickNoScroll("টেস্ট শুরু করুন")
        waitFor("প্রশ্ন ১ /", 15_000)
        tab("খ")
        compose.onNodeWithTag("markReview").performClick()
        shot("19_mock_exam")
        compose.onNodeWithTag("answerSheet").performClick()
        waitFor("উত্তরপত্র")
        shot("20_answer_sheet")
        click("জমা দিন")
        waitFor("টেস্ট জমা দেবেন?")
        shot("21_submit_confirm")
        compose.onNodeWithTag("confirmSubmit").performClick()
        waitFor("নিট নম্বর", 20_000)
        shot("22_mock_result")
    }
}
