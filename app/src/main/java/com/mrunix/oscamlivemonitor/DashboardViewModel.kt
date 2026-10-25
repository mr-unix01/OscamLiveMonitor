package com.mrunix.oscamlivemonitor

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(
    private val api: OscamApi,
    private val preferences: SharedPreferences
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> =
        _uiState.asStateFlow()

    class Factory(
        private val api: OscamApi,
        private val preferences: SharedPreferences
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            if (
                modelClass.isAssignableFrom(
                    DashboardViewModel::class.java
                )
            ) {
                return DashboardViewModel(
                    api = api,
                    preferences = preferences
                ) as T
            }

            throw IllegalArgumentException(
                "ViewModel sconosciuto: ${modelClass.name}"
            )
        }
    }
}
