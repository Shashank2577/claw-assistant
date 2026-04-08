package com.phoneclaw.ai.common

import android.net.Uri
import androidx.core.net.toUri
import net.openid.appauth.AuthorizationServiceConfiguration

const val LOCAL_URL_BASE = "https://appassets.androidplatform.net"

object ProjectConfig {
  const val HF_CLIENT_ID = "00000000-0000-0000-0000-000000000000" // Mock
  const val HF_REDIRECT_URI = "com.phoneclaw.ai.auth://oauth"
  
  private const val authEndpoint = "https://huggingface.co/oauth/authorize"
  private const val tokenEndpoint = "https://huggingface.co/oauth/token"

  val authServiceConfig =
    AuthorizationServiceConfiguration(
      authEndpoint.toUri(),
      tokenEndpoint.toUri(),
    )
}
