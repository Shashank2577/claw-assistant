package com.openclaw.ai.data.model

import com.google.gson.annotations.SerializedName

enum class Accelerator(val label: String) {
    CPU(label = "CPU"),
    GPU(label = "GPU"),
    NPU(label = "NPU"),
}

enum class RuntimeType {
    @SerializedName("unknown") UNKNOWN,
    @SerializedName("litert_lm") LITERT_LM,
}
