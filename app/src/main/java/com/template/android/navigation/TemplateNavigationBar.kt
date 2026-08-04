package com.template.android.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.template.android.feature.home.navigation.HomeKey

enum class Tab(
    val key: NavKey,
    val label: String,
) {
    Home(
        key = HomeKey,
        label = "Home",
    ),
    // Add new tabs here. Each tab needs a NavKey and a label.
    // For icons, add androidx.compose.material:material-icons-core to app/build.gradle.kts
    // and pass an icon: @Composable () -> Unit parameter.
}

@Composable
fun TemplateNavigationBar(
    currentKey: NavKey?,
    onNavigate: (NavKey) -> Unit,
) {
    NavigationBar {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentKey?.let { it::class == tab.key::class } == true,
                onClick = { onNavigate(tab.key) },
                icon = {},
                label = { Text(tab.label) },
            )
        }
    }
}
