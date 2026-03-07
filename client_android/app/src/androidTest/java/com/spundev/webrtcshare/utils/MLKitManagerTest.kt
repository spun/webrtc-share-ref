package com.spundev.webrtcshare.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_DOWNLOADING
import com.google.android.gms.common.testing.FakeModuleInstallClient
import com.google.android.gms.common.testing.FakeModuleInstallUtil
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.spundev.webrtcshare.di.MLKitModule
import com.spundev.webrtcshare.extensions.ModuleInstallState
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@UninstallModules(MLKitModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MLKitManagerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fakeModuleInstallClient = FakeModuleInstallClient(context)

    @BindValue
    @JvmField
    val moduleInstallClient: ModuleInstallClient = fakeModuleInstallClient

    @Inject
    lateinit var mlKitManager: MLKitManager

    @Before
    fun setup() {
        // trigger injection
        hiltRule.inject()
    }

    @Test
    fun checkAvailabilityWhenModuleIsReadyToDownload() = runTest {
        // Reset any previously installed modules.
        fakeModuleInstallClient.reset()

        val barcodeScannerAvailability = mlKitManager.getBarcodeScannerAvailability()
        assertEquals(AvailabilityStatus.ReadyToDownload, barcodeScannerAvailability)
    }

    @Test
    fun checkAvailabilityWhenModuleIsAvailable() = runTest {
        // Reset any previously installed modules.
        fakeModuleInstallClient.reset()

        val optionalModuleApi = GmsBarcodeScanning.getClient(context)
        fakeModuleInstallClient.setInstalledModules(optionalModuleApi)

        val barcodeScannerAvailability = mlKitManager.getBarcodeScannerAvailability()
        assertEquals(AvailabilityStatus.AlreadyAvailable, barcodeScannerAvailability)
    }

    @Test
    fun checkAvailabilityWhenModuleIsUnavailable() = runTest {
        // Reset any previously installed modules.
        fakeModuleInstallClient.reset()

        // Runtime exception when trying to get module's availability
        fakeModuleInstallClient.setModulesAvailabilityTask(Tasks.forException(RuntimeException()))

        val barcodeScannerAvailability = mlKitManager.getBarcodeScannerAvailability()
        val isUnavailable = barcodeScannerAvailability is AvailabilityStatus.Unavailable
        assertTrue(isUnavailable)
    }

    @Test
    fun urgentInstallWhenModuleAlreadyExist() = runTest {
        // Reset any previously installed modules.
        fakeModuleInstallClient.reset()

        val optionalModuleApi = GmsBarcodeScanning.getClient(context)
        fakeModuleInstallClient.setInstalledModules(optionalModuleApi)

        val barcodeScannerAvailability = mlKitManager.getBarcodeScannerAvailability()
        assertEquals(AvailabilityStatus.AlreadyAvailable, barcodeScannerAvailability)

        val receivedStates = mutableListOf<ModuleInstallState>()
        val job = launch {
            mlKitManager.installFlow(optionalModuleApi)
                .collect {
                    receivedStates.add(it)
                }
        }
        job.join()

        assertEquals(
            listOf(
                ModuleInstallState.Requested,
                ModuleInstallState.AlreadyInstalled
            ),
            receivedStates
        )
    }

    @Test
    fun urgentInstallWithListener() = runTest {
        // Reset any previously installed modules.
        fakeModuleInstallClient.reset()

        // Generate a ModuleInstallResponse and set it as the result for installModules().
        val moduleInstallResponse = FakeModuleInstallUtil.generateModuleInstallResponse()
        fakeModuleInstallClient.setInstallModulesTask(Tasks.forResult(moduleInstallResponse))

        val barcodeScannerAvailability = mlKitManager.getBarcodeScannerAvailability()
        assertEquals(AvailabilityStatus.ReadyToDownload, barcodeScannerAvailability)

        val optionalModuleApi = GmsBarcodeScanning.getClient(context)
        val receivedStates = mutableListOf<ModuleInstallState>()
        val job = launch {
            mlKitManager.installFlow(optionalModuleApi)
                .collect {
                    receivedStates.add(it)
                }
        }

        // Without this, all our sendInstallUpdates below would be executed before
        // our launch block and flow collect.
        // This would skip the COMPLETE update that makes the flow collect end and
        // the test would get stuck.
        advanceUntilIdle()

        fakeModuleInstallClient.sendInstallUpdates(
            listOf(
                FakeModuleInstallUtil.createModuleInstallStatusUpdate(
                    moduleInstallResponse.sessionId,
                    STATE_DOWNLOADING,
                    10,
                    100
                ),
                FakeModuleInstallUtil.createModuleInstallStatusUpdate(
                    moduleInstallResponse.sessionId,
                    STATE_DOWNLOADING,
                    20,
                    100
                ),
                FakeModuleInstallUtil.createModuleInstallStatusUpdate(
                    moduleInstallResponse.sessionId,
                    STATE_COMPLETED
                )
            )
        )

        job.join()

        assertEquals(
            listOf(
                ModuleInstallState.Requested,
                ModuleInstallState.Installing(10),
                ModuleInstallState.Installing(20),
                ModuleInstallState.Complete
            ),
            receivedStates
        )
    }

    @Test
    fun urgentInstallFailure() = runTest {
        fakeModuleInstallClient.setInstallModulesTask(Tasks.forException(RuntimeException()))

        // Verify the case where an RuntimeException happened when trying to send the urgent install request...
        val optionalModuleApi = GmsBarcodeScanning.getClient(context)

        val barcodeScannerAvailability = mlKitManager.getBarcodeScannerAvailability()
        assertEquals(AvailabilityStatus.ReadyToDownload, barcodeScannerAvailability)

        val receivedStates = mutableListOf<ModuleInstallState>()
        val job = launch {
            mlKitManager.installFlow(optionalModuleApi)
                .catch { println("Install failed") }
                .collect {
                    receivedStates.add(it)
                }
        }
        job.join()

        assertEquals(
            listOf<ModuleInstallState>(),
            receivedStates
        )
    }
}
