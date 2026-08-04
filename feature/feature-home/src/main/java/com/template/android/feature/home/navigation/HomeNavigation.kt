@file:Suppress("MatchingDeclarationName")

package com.template.android.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.template.android.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

fun EntryProviderScope<NavKey>.homeDestination(
    onItemClick: (String) -> Unit = {},
) {
    entry<HomeKey> { HomeScreen(onItemClick = onItemClick) }
}
