package com.novacorp.inmonode_app.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(reservations: ReservationRepository) : ViewModel() {

    /** Separations whose lot went to another operation, shown on the Sync tab from any screen (US-12). */
    val unresolvedConflicts: StateFlow<Int> = reservations.observeReservations()
        .map { rows -> rows.count { it.awaitsReassignment } }
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
