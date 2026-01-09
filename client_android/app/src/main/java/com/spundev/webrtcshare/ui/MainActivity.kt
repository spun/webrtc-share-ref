package com.spundev.webrtcshare.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.spundev.webrtcshare.ui.screens.createRoom.CreateRoomRoute
import com.spundev.webrtcshare.ui.screens.joinRoom.JoinRoomRoute
import com.spundev.webrtcshare.ui.screens.main.MainRoute
import com.spundev.webrtcshare.ui.theme.WebRTCShareTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
object MainRoute : NavKey

@Serializable
object CreateRoomRoute : NavKey

@Serializable
object JoinRoomRoute : NavKey

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebRTCShareTheme {
                Surface {
                    NavProvider()
                }
            }
        }
    }
}

@Composable
private fun NavProvider() {

    val backStack = rememberNavBackStack(MainRoute)
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            // Add the default decorators for managing scenes and saving state
            rememberSaveableStateHolderNavEntryDecorator(),
            // Then add the view model store decorator
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<MainRoute> {
                MainRoute(
                    onNavigateToCreate = { backStack.add(CreateRoomRoute) },
                    onNavigateToJoin = { backStack.add(JoinRoomRoute) },
                )
            }
            entry<CreateRoomRoute> {
                CreateRoomRoute(onNavigateBack = { backStack.removeLastOrNull() })
            }
            entry<JoinRoomRoute> {
                JoinRoomRoute(onNavigateBack = { backStack.removeLastOrNull() })
            }
        }
    )
}
