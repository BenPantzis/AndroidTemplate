package com.template.android.feature.home

sealed interface HomeAction {
    data object LoadContent : HomeAction
}

sealed interface HomeEvent {
    data class ShowSnackbar(val message: String) : HomeEvent
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val message: String = "",
)
