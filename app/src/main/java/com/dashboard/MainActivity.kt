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

        val dashboard = DrawDashboard()

        setContent {
            var currentScreen by rememberSaveable { mutableStateOf("menu") }

            when (currentScreen) {
                "menu" -> {
                    StartupMenu(onGameSelected = { gameType, ip ->
                        if (gameType == GameType.GT7) {
                            viewModel.startGt7Tracking(ip)
                        } else {
                            viewModel.startACTracking(ip)
                        }
                        currentScreen = "dashboard"
                    })
                }
                "dashboard" -> {
                    dashboard.DashboardScreen(viewModel = viewModel)
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
    val lapTimeState = mutableIntStateOf(0)
    val lapTimeBestState = mutableIntStateOf(0)
    val lapTimeLastState = mutableIntStateOf(0)

    private val acParser = ACParser()
    fun startACTracking(targetIp: String) {
        viewModelScope.launch {
            acParser.connectToAssettoCorsa(targetIp) { speed, rpm, maxRpm, gear, isAbs, isTc, lapTime, lapTimeBest, lapTimeLast ->
                speedState.floatValue = speed
                rpmState.floatValue = rpm
                maxRpmState.floatValue = maxRpm
                gearState.value = gear
                absState.value = isAbs
                tcState.value = isTc
                lapTimeState.intValue = lapTime
                lapTimeBestState.intValue = lapTimeBest
                lapTimeLastState.intValue = lapTimeLast
            }
        }
    }

    private val gt7Parser = GT7Parser()
    fun startGt7Tracking(targetIp: String) {
        viewModelScope.launch {
            gt7Parser.connectToGranTurismo7(targetIp) { speed, rpm, maxRpm, gear, isAbs, isTc, lapTime, lapTimeBest, lapTimeLast ->
                speedState.floatValue = speed
                rpmState.floatValue = rpm
                maxRpmState.floatValue = maxRpm
                gearState.value = gear
                absState.value = isAbs
                tcState.value = isTc
                lapTimeState.intValue = lapTime
                lapTimeBestState.intValue = lapTimeBest
                lapTimeLastState.intValue = lapTimeLast
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gt7Parser.stop()
        acParser.stop()
    }
}