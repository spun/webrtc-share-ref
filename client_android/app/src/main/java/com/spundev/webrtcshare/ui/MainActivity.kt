package com.spundev.webrtcshare.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.spundev.webrtcshare.ui.screens.createRoom.CreateRoomRoute
import com.spundev.webrtcshare.ui.screens.joinRequest.JoinRequestRoute
import com.spundev.webrtcshare.ui.screens.joinRoom.JoinRoomRoute
import com.spundev.webrtcshare.ui.screens.joinRoom.JoinRoomViewModel
import com.spundev.webrtcshare.ui.screens.localDemo.LocalDemoRoute
import com.spundev.webrtcshare.ui.screens.main.MainRoute
import com.spundev.webrtcshare.ui.theme.WebRTCShareTheme
import com.spundev.webrtcshare.utils.materialSharedAxisIn
import com.spundev.webrtcshare.utils.materialSharedAxisOut
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
object MainRoute : NavKey

@Serializable
object LocalDemoRoute : NavKey

@Serializable
object CreateRoomRoute : NavKey

@Serializable
object JoinRequestRoute : NavKey

@Serializable
data class JoinRoomRoute(val roomId: String) : NavKey

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
    val density = LocalDensity.current
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
                    onNavigateToLocalDemo = dropUnlessResumed { backStack.add(LocalDemoRoute) },
                    onNavigateToCreate = dropUnlessResumed { backStack.add(CreateRoomRoute) },
                    onNavigateToJoinRequest = dropUnlessResumed { backStack.add(JoinRequestRoute) },
                )
            }
            entry<LocalDemoRoute> {
                LocalDemoRoute(onNavigateBack = dropUnlessResumed { backStack.removeLastOrNull() })
            }
            entry<CreateRoomRoute> {
                CreateRoomRoute(onNavigateBack = dropUnlessResumed { backStack.removeLastOrNull() })
            }
            entry<JoinRequestRoute> {
                JoinRequestRoute(
                    onNavigateBack = dropUnlessResumed { backStack.removeLastOrNull() },
                    onNavigateToRoom = { roomId ->
                        backStack.add(JoinRoomRoute(roomId))
                    }
                )
            }
            entry<JoinRoomRoute> {
                JoinRoomRoute(
                    onNavigateBack = dropUnlessResumed { backStack.removeLastOrNull() },
                    viewModel = hiltViewModel<JoinRoomViewModel, JoinRoomViewModel.Factory>(
                        creationCallback = { factory -> factory.create(it.roomId) }
                    )
                )
            }
        },
        transitionSpec = {
            // Slide in from right when navigating forward
            materialSharedAxisIn(
                slideDirection = AnimatedContentTransitionScope.SlideDirection.Left,
                density = density
            ) togetherWith materialSharedAxisOut(
                slideDirection = AnimatedContentTransitionScope.SlideDirection.Left,
                density = density
            )
        },
        popTransitionSpec = {
            // Slide in from left when navigating back
            materialSharedAxisIn(
                slideDirection = AnimatedContentTransitionScope.SlideDirection.Right,
                density = density
            ) togetherWith materialSharedAxisOut(
                slideDirection = AnimatedContentTransitionScope.SlideDirection.Right,
                density = density
            )
        },
        predictivePopTransitionSpec = {
            // Slide in from left when navigating back
            materialSharedAxisIn(
                slideDirection = AnimatedContentTransitionScope.SlideDirection.Right,
                density = density
            ) togetherWith materialSharedAxisOut(
                slideDirection = AnimatedContentTransitionScope.SlideDirection.Right,
                density = density
            )
        }
    )
}
