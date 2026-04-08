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

package com.phoneclaw.ai.data

import android.os.Build
import com.google.gson.annotations.SerializedName

/** Stub for hardware check. */
private fun isPixel10(): Boolean = false

/**
 * Represents the allowlist of models that are allowed to be downloaded and used in the app.
 */
data class ModelAllowlist(
  @SerializedName("models") val models: List<ModelAllowlistEntry> = emptyList()
)

data class ModelAllowlistEntry(
  @SerializedName("name") val name: String,
  @SerializedName("version") val version: String,
  @SerializedName("displayName") val displayName: String,
  @SerializedName("info") val info: String,
  @SerializedName("url") val url: String,
  @SerializedName("sizeInBytes") val sizeInBytes: Long,
  @SerializedName("llmMaxToken") val llmMaxToken: Int = 512,
  @SerializedName("llmSupportImage") val llmSupportImage: Boolean = false,
  @SerializedName("llmSupportAudio") val llmSupportAudio: Boolean = false,
  @SerializedName("isLlm") val isLlm: Boolean = true,
  @SerializedName("isExperimental") val isExperimental: Boolean = false,
  @SerializedName("accelerators") val accelerators: List<String> = emptyList(),
  @SerializedName("visionAccelerator") val visionAccelerator: String = "CPU",
  @SerializedName("disabled") val disabled: Boolean = false,
  @SerializedName("pixel10Only") val pixel10Only: Boolean = false,
) {
  fun toModel(): Model {
    return Model(
      name = name,
      version = version,
      displayName = displayName,
      info = info,
      url = url,
      sizeInBytes = sizeInBytes,
      llmMaxToken = llmMaxToken,
      llmSupportImage = llmSupportImage,
      llmSupportAudio = llmSupportAudio,
      isLlm = isLlm,
      accelerators =
        accelerators.map {
          when (it.uppercase()) {
            "GPU" -> Accelerator.GPU
            "NPU" -> Accelerator.NPU
            else -> Accelerator.CPU
          }
        },
      visionAccelerator =
        when (visionAccelerator.uppercase()) {
          "GPU" -> Accelerator.GPU
          "NPU" -> Accelerator.NPU
          else -> Accelerator.CPU
        }
    )
  }

  fun isCompatible(): Boolean {
    if (disabled) return false
    if (pixel10Only && !isPixel10()) return false
    return true
  }
}
