/*
 * Copyright 2025 Google LLC
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

package com.phoneclaw.ai.customtasks.common

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phoneclaw.ai.ui.settings.SettingsViewModel

/**
 * Data class to hold information passed to the `MainScreen` composable of a custom task.
 *
 * @param settingsViewModel The ViewModel providing access to the state of models and their
 *   management.
 */
data class CustomTaskData(
  val settingsViewModel: SettingsViewModel,
  val bottomPadding: Dp = 0.dp,
  val setAppBarControlsDisabled: (Boolean) -> Unit = {},
  val setTopBarVisible: (Boolean) -> Unit = {},
  val setCustomNavigateUpCallback: ((() -> Unit)?) -> Unit = {},
)

data class CustomTaskDataForBuiltinTask(
  val settingsViewModel: SettingsViewModel,
  val onNavUp: () -> Unit,
)
