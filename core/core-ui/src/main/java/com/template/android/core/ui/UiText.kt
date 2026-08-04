package com.template.android.core.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    class StringResource(
        @param:StringRes val id: Int,
        vararg val args: Any,
    ) : UiText

    data class Plain(val value: String) : UiText

    fun resolve(context: Context): String = when (this) {
        is StringResource -> context.getString(id, *args)
        is Plain -> value
    }

    @Composable
    fun asString(): String = when (this) {
        is StringResource -> stringResource(id, *args)
        is Plain -> value
    }
}
