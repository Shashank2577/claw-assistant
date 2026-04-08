package com.phoneclaw.ai.runtime

import com.phoneclaw.ai.data.Model

/**
 * Extension functions for [LlmModelHelper].
 */

/**
 * Runs inference for the given [model].
 */
fun LlmModelHelper.runInference(
  model: Model,
  input: String,
  resultListener: ResultListener,
  cleanUpListener: CleanUpListener,
  onError: (message: String) -> Unit,
) {
  runInference(
    model = model,
    input = input,
    resultListener = resultListener,
    cleanUpListener = cleanUpListener,
    onError = onError,
    images = listOf(),
    audioClips = listOf(),
  )
}
