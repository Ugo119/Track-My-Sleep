/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 * The AndroidViewModel class is the same as ViewModel, but it takes the application context
 * as a constructor parameter and makes it available as a property.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {
        private var tonight = MutableLiveData<SleepNight?>()
        val nights = database.getAllNights()

        val nightsString = Transformations.map(nights) { nights ->
                formatNights(nights, application.resources)
        }

        private val _navigateToSleepQuality = MutableLiveData<SleepNight?>()

        val navigateToSleepQuality: LiveData<SleepNight?>
                get() = _navigateToSleepQuality

        //The Start button should be enabled when tonight is null.
        val startButtonVisible = Transformations.map(tonight) {
                it == null
        }

        //The Stop button should be enabled when tonight is not null.
        val stopButtonVisible = Transformations.map(tonight) {
                it != null
        }

        //The Clear button should only be enabled if nights, and thus the database,
        //contains sleep nights.
        val clearButtonVisible = Transformations.map(nights) {
                it?.isNotEmpty()
        }

        private var _showSnackbarEvent = MutableLiveData<Boolean>()

        val showSnackBarEvent: LiveData<Boolean>
                get() = _showSnackbarEvent

        init {
            initializeTonight()
        }

        private fun initializeTonight() {
                // Use the viewModelScope.launch to start a coroutine in the ViewModelScope
                //Notice the use of curly braces for launch. They are defining a lambda expression,
                // which is a function without a name. In this example, you are passing in a
                // lambda to the launch coroutine builder. This builder creates a coroutine and
                // assigns the execution of that lambda to the corresponding dispatcher.
                viewModelScope.launch {
                        tonight.value = getTonightFromDatabase()
                }
        }

        private suspend fun getTonightFromDatabase(): SleepNight? {
                //This function returns a NEW night.
                var night = database.getTonight()
                //Remember that the start and end times for a NEW night are the same!
                if (night?.endTimeMilli != night?.startTimeMilli) {
                        night = null
                }
                return night
        }

        fun onStartTracking() {
                viewModelScope.launch {
                        val newNight = SleepNight()
                        insert(newNight)
                        tonight.value = getTonightFromDatabase()

                }
        }

        fun onStopTracking() {
                viewModelScope.launch {
                        val oldNight = tonight.value ?: return@launch
                        oldNight.endTimeMilli = System.currentTimeMillis()
                        update(oldNight)
                        _navigateToSleepQuality.value = oldNight
                }
        }

        fun onClear() {
                viewModelScope.launch {
                        clear()
                        tonight.value = null
                }
        }

        fun doneNavigating() {
                _navigateToSleepQuality.value = null
        }

        fun doneShowingSnackbar() {
                _showSnackbarEvent.value = false
        }

        private suspend fun insert(night: SleepNight) {
                database.insert(night)
        }

        private suspend fun update(night: SleepNight) {
                database.update(night)
        }

        suspend fun clear() {
                database.clear()
        }


}

