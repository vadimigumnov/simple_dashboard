package com.dashboard

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val regularDashboard = RegularDashboard()
        val rallyDashboard = RallyDashboard()

        setContent {
            var currentScreen by rememberSaveable { mutableStateOf("menu") }
            var activePreset by rememberSaveable { mutableStateOf<GameType?>(null) }

            when (currentScreen) {
                "menu" -> {
                    StartupMenu(onGameSelected = { gameType, ip ->

                        activePreset = gameType
                        when (gameType) {
                            GameType.GT7 -> viewModel.startGt7Tracking(ip)
                            GameType.AC -> viewModel.startACTracking(ip)
                            GameType.RBR -> viewModel.startRBRTracking(ip)
                        }
                        currentScreen = "dashboard"
                    })
                }
                "dashboard" -> {
                    when(activePreset) {
                        GameType.GT7 -> regularDashboard.DashboardScreen(viewModel = viewModel)
                        GameType.AC -> regularDashboard.DashboardScreen(viewModel = viewModel)
                        GameType.RBR -> rallyDashboard.DashboardScreen(viewModel = viewModel)
                        null -> {}
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
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
    fun startACTracking(targetIp: String) {
        viewModelScope.launch {
            acParser.connectToAssettoCorsa(targetIp) { speed, rpm, maxRpm, gear, isAbs, isTc, time0, time1, time2, param, timeStamp ->
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
        }
    }

    private val gt7Parser = GT7Parser()
    fun startGt7Tracking(targetIp: String) {
        viewModelScope.launch {
            gt7Parser.connectToGranTurismo7(targetIp) { speed, rpm, maxRpm, gear, isAbs, isTc, time0, time1, time2, param, timeStamp ->
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
        }
    }

    private val rbrParser = RBRParser()
    fun startRBRTracking(targetIp: String) {
        viewModelScope.launch {
            rbrParser.connectToRBR(targetIp) { speed, rpm, maxRpm, gear, isAbs, isTc, time0, time1, time2, param, timeStamp ->
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
        }
    }

    override fun onCleared() {
        super.onCleared()
        gt7Parser.stop()
        acParser.stop()
        rbrParser.stop()
    }
}