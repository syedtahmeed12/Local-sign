package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.SignatureViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Document Signer", appName)
  }

  @Test
  fun `launch MainActivity`() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      assert(true)
    }
  }

  @Test
  fun `test DashboardScreen composition`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = SignatureViewModel(application)
    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }
}
