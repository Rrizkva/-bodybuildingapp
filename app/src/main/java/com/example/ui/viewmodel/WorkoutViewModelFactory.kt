package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.WorkoutRepository

class WorkoutViewModelFactory(
    private val repository: WorkoutRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PlanEditorViewModel::class.java) -> {
                PlanEditorViewModel(repository) as T
            }
            modelClass.isAssignableFrom(WorkoutViewModel::class.java) -> {
                WorkoutViewModel(repository) as T
            }
            modelClass.isAssignableFrom(AnalysisViewModel::class.java) -> {
                AnalysisViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
