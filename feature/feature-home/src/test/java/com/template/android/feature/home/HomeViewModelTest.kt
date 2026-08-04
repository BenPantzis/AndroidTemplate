package com.template.android.feature.home

import app.cash.turbine.test
import com.template.android.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        viewModel = HomeViewModel()
    }

    @Test
    fun `initial state has isLoading true`() = runTest {
        val initial = viewModel.uiState.value
        assertTrue(initial.isLoading)
    }

    @Test
    fun `load content sets message and clears loading`() = runTest {
        viewModel.uiState.test {
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertTrue(loaded.message.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `load content emits expected message`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Hello, World!", state.message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
