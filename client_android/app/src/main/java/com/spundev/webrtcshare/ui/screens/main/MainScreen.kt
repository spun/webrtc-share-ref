package com.spundev.webrtcshare.ui.screens.main

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spundev.webrtcshare.R
import com.spundev.webrtcshare.ui.theme.WebRTCShareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainRoute(
    onNavigateToLocalDemo: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToJoinRequest: () -> Unit,
) {
    Column {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },
            colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent)
        )

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.width(IntrinsicSize.Max)
            ) {
                // Create room button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = ButtonBigRadius,
                                topEnd = ButtonBigRadius,
                                bottomEnd = ButtonSmallRadius,
                                bottomStart = ButtonSmallRadius
                            )
                        )
                        .clickable(onClick = onNavigateToCreate)
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .height(72.dp)
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 24.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_add_24), contentDescription = null)
                    Text(stringResource(R.string.main_create_room))
                }

                // Join room button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = ButtonSmallRadius,
                                topEnd = ButtonSmallRadius,
                                bottomEnd = ButtonBigRadius,
                                bottomStart = ButtonBigRadius
                            )
                        )
                        .clickable(onClick = onNavigateToJoinRequest)
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .height(72.dp)
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 24.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_open_24), contentDescription = null)
                    Text(stringResource(R.string.main_join_room))
                }

            }
            Spacer(modifier = Modifier.height(8.dp))
            // Local demo button
            TextButton(onClick = onNavigateToLocalDemo) {
                Text(stringResource(R.string.main_launch_local_demo))
            }
        }
    }
}

private val ButtonBigRadius = 16.dp
private val ButtonSmallRadius = 4.dp


@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun MainScreenPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            MainRoute(
                onNavigateToLocalDemo = { },
                onNavigateToCreate = { },
                onNavigateToJoinRequest = { }
            )
        }
    }
}
