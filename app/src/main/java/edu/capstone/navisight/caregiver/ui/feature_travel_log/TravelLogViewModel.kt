package edu.capstone.navisight.caregiver.ui.feature_travel_log

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.capstone.navisight.caregiver.domain.travelLogUseCase.GetTravelLogsUseCase
import edu.capstone.navisight.caregiver.model.GeofenceActivity
import edu.capstone.navisight.common.CsvExportUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TravelLogViewModel(
    private val getTravelLogsUseCase: GetTravelLogsUseCase = GetTravelLogsUseCase()
) : ViewModel() {

    private val _logs = MutableStateFlow<List<GeofenceActivity>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var activeViuUid: String? = null
    private var fetchJob: Job? = null

    fun loadLogs(viuUid: String) {
        if (activeViuUid == viuUid && (_logs.value.isNotEmpty() || _isLoading.value)) {
            return
        }

        activeViuUid = viuUid
        fetchJob?.cancel()

        _isLoading.value = true

        fetchJob = viewModelScope.launch {
            getTravelLogsUseCase(viuUid).collect {
                _logs.value = it
                _isLoading.value = false
            }
        }
    }

    fun exportToCsv(context: Context, viuName: String) {
        if (_logs.value.isNotEmpty()) {
            CsvExportUtil.exportAndShare(context, _logs.value, viuName)
        }
    }
}