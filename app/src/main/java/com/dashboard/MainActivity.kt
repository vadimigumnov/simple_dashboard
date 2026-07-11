package com.dashboard

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SimHubTelemetry(
    val timeStamp: Long,
    val maxRpm: Float,
    val rpmValue: Float,
    val speed: Float,
    val absActive: Boolean,
    val tcActive: Boolean,
    val gear: String,
    val currentLapTime: Int,
    val lastLapTime: Int,
    val bestLapTime: Int
)

class MainActivity : ComponentActivity() {
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val gt7Dashboard = GT7Dashboard()
        val simpleDashboard = SimpleDashboard()
        val rallyDashboard = RallyDashboard()

        setContent {
            var currentScreen by rememberSaveable { mutableStateOf("menu") }
            var activePreset by rememberSaveable { mutableStateOf<GameType?>(null) }

            when (currentScreen) {
                "menu" -> {
                    StartupMenu(onGameSelected = { gameType, ip, port ->
                        activePreset = gameType
                        when (gameType) {
                            GameType.GT7 -> viewModel.startGt7Tracking(ip, port)
                            GameType.AC -> viewModel.startACTracking(ip, port)
                            GameType.RBR -> viewModel.startRBRTracking(ip, port)
                            GameType.SIMHUB -> viewModel.startSimHubTracking(ip, port)
                        }
                        currentScreen = "dashboard"
                    })
                }

                "dashboard" -> {
                    if (viewModel.timeStampState.longValue == 0L) {

                        WaitingForDataScreen(
                            onBackToMenu = {
                                viewModel.resetAndStopAll()
                                activePreset = null
                                currentScreen = "menu"
                            }
                        )

                    } else {
                        when (activePreset) {
                            GameType.GT7 -> gt7Dashboard.DashboardScreen(viewModel = viewModel)
                            GameType.AC -> simpleDashboard.DashboardScreen(viewModel = viewModel)
                            GameType.RBR -> rallyDashboard.DashboardScreen(viewModel = viewModel)
                            GameType.SIMHUB -> DashboardScreen(viewModel = viewModel)
                            null -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class DashboardViewModel : ViewModel() {

    val speedState = mutableFloatStateOf(0f)
    val rpmState = mutableFloatStateOf(0f)
    val maxRpmState = mutableFloatStateOf(0f)
    val gearState = mutableStateOf("N")
    val absState = mutableStateOf(false)
    val tcState = mutableStateOf(false)
    val time0State = mutableIntStateOf(0)
    val time1State = mutableIntStateOf(0)
    val time2State = mutableIntStateOf(0)
    val paramState = mutableIntStateOf(0)
    val timeStampState = mutableLongStateOf(0L)

    private val acParser = ACParser()
    fun startACTracking(targetIp: String, port: String) {
        viewModelScope.launch {
            acParser.connectToAssettoCorsa(
                targetIp,
                port
            ) { speed, rpm, maxRpm, gear, isAbs, isTc, time0, time1, time2, param, timeStamp ->
                updateStates(
                    speed,
                    rpm,
                    maxRpm,
                    gear,
                    isAbs,
                    isTc,
                    time0,
                    time1,
                    time2,
                    param,
                    timeStamp
                )
            }
        }
    }

    private val gt7Parser = GT7Parser()
    fun startGt7Tracking(targetIp: String, port: String) {
        viewModelScope.launch {
            gt7Parser.connectToGranTurismo7(
                targetIp,
                port
            ) { speed, rpm, maxRpm, gear, isAbs, isTc, time0, time1, time2, param, timeStamp ->
                updateStates(
                    speed,
                    rpm,
                    maxRpm,
                    gear,
                    isAbs,
                    isTc,
                    time0,
                    time1,
                    time2,
                    param,
                    timeStamp
                )
            }
        }
    }

    private val rbrParser = RBRParser()
    fun startRBRTracking(targetIp: String, port: String) {
        viewModelScope.launch {
            rbrParser.connectToRBR(
                targetIp,
                port
            ) { speed, rpm, maxRpm, gear, isAbs, isTc, time0, time1, time2, param, timeStamp ->
                updateStates(
                    speed,
                    rpm,
                    maxRpm,
                    gear,
                    isAbs,
                    isTc,
                    time0,
                    time1,
                    time2,
                    param,
                    timeStamp
                )
            }
        }
    }

    private var connectToSimHub: SimHubParser? = null

    fun startSimHubTracking(targetIp: String, targetPort: String) {
        connectToSimHub?.stop()

        connectToSimHub = SimHubParser(targetIp, targetPort) { telemetry ->
            viewModelScope.launch(Dispatchers.Main) {
                speedState.floatValue = telemetry.speed
                rpmState.floatValue = telemetry.rpmValue
                maxRpmState.floatValue = telemetry.maxRpm
                gearState.value = telemetry.gear
                absState.value = telemetry.absActive
                tcState.value = telemetry.tcActive
                timeStampState.longValue = telemetry.timeStamp
                time0State.intValue = telemetry.currentLapTime
                time1State.intValue = telemetry.lastLapTime
                time2State.intValue = telemetry.bestLapTime
            }
        }
        connectToSimHub?.start()
    }

    private fun updateStates(
        speed: Float, rpm: Float, maxRpm: Float, gear: String,
        isAbs: Boolean, isTc: Boolean, time0: Int, time1: Int,
        time2: Int, param: Int, timeStamp: Long
    ) {
        speedState.floatValue = speed
        rpmState.floatValue = rpm
        maxRpmState.floatValue = maxRpm
        gearState.value = gear
        absState.value = isAbs
        tcState.value = isTc
        time0State.intValue = time0
        time1State.intValue = time1
        time2State.intValue = time2
        paramState.intValue = param
        timeStampState.longValue = timeStamp
    }

    override fun onCleared() {
        super.onCleared()
        gt7Parser.stop()
        acParser.stop()
        rbrParser.stop()
        connectToSimHub?.stop()
    }

    fun resetAndStopAll() {
        gt7Parser.stop()
        acParser.stop()
        rbrParser.stop()
        connectToSimHub?.stop()

        timeStampState.longValue = 0L
        speedState.floatValue = 0f
        rpmState.floatValue = 0f
    }
}

