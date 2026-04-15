/*
 * Copyright 2026 Google LLC
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
package com.phoneclaw.ai.customtasks.agentchat

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.lang.Exception

@JsonClass(generateAdapter = true)
data class SendEmailParams(
  val extra_email: String,
  val extra_subject: String,
  val extra_text: String,
)

@JsonClass(generateAdapter = true)
data class SendSmsParams(val phone_number: String, val sms_body: String)

@JsonClass(generateAdapter = true)
data class MakeCallParams(val phone_number: String)

@JsonClass(generateAdapter = true)
data class SetAlarmParams(val hour: Int, val minute: Int, val message: String = "")

@JsonClass(generateAdapter = true)
data class OpenAppParams(val package_name: String)

@JsonClass(generateAdapter = true)
data class OpenUrlParams(val url: String)

@JsonClass(generateAdapter = true)
data class WriteCalendarParams(
  val title: String,
  val begin_time: Long,
  val end_time: Long,
  val location: String = "",
  val description: String = "",
)

object IntentHandler {
  private const val TAG = "IntentHandler"

  fun handleAction(context: Context, action: String, parameters: String): Boolean {
    if (action == "send_email") {
      try {
        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(SendEmailParams::class.java)
        val params = jsonAdapter.fromJson(parameters)
        if (params != null) {
          val intent =
            Intent(Intent.ACTION_SEND).apply {
              setDataAndType("mailto:".toUri(), "text/plain")
              putExtra(Intent.EXTRA_EMAIL, arrayOf(params.extra_email))
              putExtra(Intent.EXTRA_SUBJECT, params.extra_subject)
              putExtra(Intent.EXTRA_TEXT, params.extra_text)
            }
          context.startActivity(intent)
          return true
        } else {
          Log.e(TAG, "Failed to parse send_email parameters: $parameters")
          return false
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to parse send_email parameters: $parameters", e)
        return false
      }
    } else if (action == "send_sms") {
      try {
        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(SendSmsParams::class.java)
        val params = jsonAdapter.fromJson(parameters)
        if (params != null) {
          val uri = "smsto:${params.phone_number}".toUri()
          val intent = Intent(Intent.ACTION_SENDTO, uri)
          intent.putExtra("sms_body", params.sms_body)
          context.startActivity(intent)
          return true
        } else {
          Log.e(TAG, "Failed to parse send_sms parameters: $parameters")
          return false
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to parse send_sms parameters: $parameters", e)
        return false
      }
    } else if (action == "make_call") {
      try {
        val moshi = Moshi.Builder().build()
        val params = moshi.adapter(MakeCallParams::class.java).fromJson(parameters)
        if (params != null) {
          val intent = Intent(Intent.ACTION_DIAL, "tel:${params.phone_number}".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(intent)
          return true
        }
        return false
      } catch (e: Exception) {
        Log.e(TAG, "Failed to parse make_call parameters: $parameters", e)
        return false
      }
    } else if (action == "set_alarm") {
      try {
        val moshi = Moshi.Builder().build()
        val params = moshi.adapter(SetAlarmParams::class.java).fromJson(parameters)
        if (params != null) {
          val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(android.provider.AlarmClock.EXTRA_HOUR, params.hour)
            putExtra(android.provider.AlarmClock.EXTRA_MINUTES, params.minute)
            putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, params.message)
            putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(intent)
          return true
        }
        return false
      } catch (e: Exception) {
        Log.e(TAG, "Failed to parse set_alarm parameters: $parameters", e)
        return false
      }
    } else if (action == "open_app") {
      try {
        val moshi = Moshi.Builder().build()
        val params = moshi.adapter(OpenAppParams::class.java).fromJson(parameters)
        if (params != null) {
          val intent = context.packageManager.getLaunchIntentForPackage(params.package_name)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          if (intent != null) {
            context.startActivity(intent)
            return true
          }
          Log.e(TAG, "App not found: ${params.package_name}")
          return false
        }
        return false
      } catch (e: Exception) {
        Log.e(TAG, "Failed to parse open_app parameters: $parameters", e)
        return false
      }
    } else if (action == "open_url") {
      try {
        val moshi = Moshi.Builder().build()
        val params = moshi.adapter(OpenUrlParams::class.java).fromJson(parameters)
        if (params != null) {
          val intent = Intent(Intent.ACTION_VIEW, params.url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(intent)
          return true
        }
        return false
      } catch (e: Exception) {
        Log.e(TAG, "Failed to parse open_url parameters: $parameters", e)
        return false
      }
    } else if (action == "write_calendar") {
      try {
        val moshi = Moshi.Builder().build()
        val params = moshi.adapter(WriteCalendarParams::class.java).fromJson(parameters)
        if (params != null) {
          val intent = Intent(Intent.ACTION_INSERT, android.provider.CalendarContract.Events.CONTENT_URI).apply {
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, params.begin_time)
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, params.end_time)
            putExtra(android.provider.CalendarContract.Events.TITLE, params.title)
            putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, params.location)
            putExtra(android.provider.CalendarContract.Events.DESCRIPTION, params.description)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(intent)
          return true
        }
        return false
      } catch (e: Exception) {
        Log.e(TAG, "Failed to parse write_calendar parameters: $parameters", e)
        return false
      }
    }
    return false
  }
}
