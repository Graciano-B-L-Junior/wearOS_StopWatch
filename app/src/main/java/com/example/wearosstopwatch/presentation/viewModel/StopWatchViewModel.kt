@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.wearosstopwatch.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wearosstopwatch.presentation.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class StopWatchViewModel: ViewModel() {
    private val _elipsedTime = MutableStateFlow(0L)
    private var timeState = MutableStateFlow(TimerState.RESET)
    val timerState = timeState.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")
    val stopWatch = _elipsedTime
        .map {
            milis ->
            LocalTime.ofNanoOfDay(milis * 1_000_000).format(formatter)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "00:00:00:000"
        )
    init {
        timerState
            .flatMapLatest { timerState->
                getTimerFlow(
                    isRunning = timerState == TimerState.RUNNING
                )
            }
            .onEach {timeDiff->
                _elipsedTime.update { it+timeDiff }
            }
            .launchIn(viewModelScope)
    }

    fun toggleIsRunning(){
        when(timerState.value){
            TimerState.RUNNING -> timeState.update { TimerState.PAUSED }
            TimerState.PAUSED,
            TimerState.RESET -> timeState.update { TimerState.RUNNING }
        }
    }

    fun resetTimer(){
        timeState.update { TimerState.RESET }
        _elipsedTime.update { 0L }
    }

    private fun getTimerFlow(isRunning: Boolean): Flow<Long>{
        return flow {
            var startMillis = System.currentTimeMillis()
            while (isRunning){
                val currentMillis = System.currentTimeMillis()
                val timeDiff = if(currentMillis > startMillis){
                    currentMillis - startMillis
                }else 0L
                emit(timeDiff)
                startMillis = System.currentTimeMillis()
                delay(10L)
            }
        }
    }
}