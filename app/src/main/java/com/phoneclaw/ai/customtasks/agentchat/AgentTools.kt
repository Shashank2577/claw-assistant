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
import android.util.Log
import com.phoneclaw.ai.common.AgentAction
import com.phoneclaw.ai.common.AskInfoAgentAction
import com.phoneclaw.ai.common.RequestPermissionAgentAction
import com.phoneclaw.ai.common.CallJsAgentAction
import com.phoneclaw.ai.common.CallJsSkillResult
import com.phoneclaw.ai.common.CallJsSkillResultImage
import com.phoneclaw.ai.common.CallJsSkillResultWebview
import com.phoneclaw.ai.common.LOCAL_URL_BASE
import com.phoneclaw.ai.common.SkillProgressAgentAction
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking

private const val TAG = "AGAgentTools"

class AgentTools @javax.inject.Inject constructor() : ToolSet {
  lateinit var context: Context
  lateinit var skillManagerViewModel: SkillManagerViewModel

  private val _actionChannel = Channel<AgentAction>(Channel.UNLIMITED)
  val actionChannel: ReceiveChannel<AgentAction> = _actionChannel
  var resultImageToShow: CallJsSkillResultImage? = null
  var resultWebviewToShow: CallJsSkillResultWebview? = null

  /** Loads skill. */
  @Tool(description = "Loads a skill.")
  fun loadSkill(
    @ToolParam(description = "The name of the skill to load.") skillName: String
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      val skills = skillManagerViewModel.getSelectedSkills()
      val skill = skills.find { it.name == skillName.trim() }
      val skillContent =
        if (skill != null) {
          "---\nname: ${skill.name}\ndescription: ${skill.description}\n---\n\n${skill.instructions}"
        } else {
          "Skill not found"
        }
      Log.d(TAG, "load skill. Skill content:\n$skillContent")
      if (skill != null) {
        _actionChannel.send(
          SkillProgressAgentAction(
            label = "Loading skill \"$skillName\"",
            inProgress = true,
            addItemTitle = "Load \"${skill.name}\"",
            addItemDescription = "Description: ${skill.description}",
            customData = skill,
          )
        )
      } else {
        _actionChannel.send(
          SkillProgressAgentAction(
            label = "Failed to load skill \"$skillName\"",
            inProgress = false,
          )
        )
      }

      mapOf("skill_name" to skillName, "skill_instructions" to skillContent)
    }
  }

  /** Call JS skill */
  @Tool(description = "Runs JS script")
  fun runJs(
    @ToolParam(description = "The name of skill") skillName: String,
    @ToolParam(description = "The script name to run. Use 'index.html' if not provided by user")
    scriptName: String,
    @ToolParam(
      description = "The data to pass to the script. Use empty string if not provided by user"
    )
    data: String,
  ): Map<String, Any> {
    return runBlocking(Dispatchers.Default) {
      Log.d(
        TAG,
        "runJS tool called with:" +
          "\n- skillName: ${skillName}\n- scriptName: ${scriptName}\n- data: ${data}\n",
      )

      val skills = skillManagerViewModel.getSelectedSkills()
      val skill = skills.find { it.name == skillName.trim() }

      if (skill == null) {
        _actionChannel.send(
          SkillProgressAgentAction(
            label = "Failed to call skill \"$scriptName\"",
            inProgress = false,
          )
        )
        return@runBlocking mapOf(
          "error" to "Skill \"${scriptName}\" not found",
          "status" to "failed",
        )
      }

      // Check secret. If a skill requires a secret and the secret is not provided, show error.
      var secret = ""
      if (skill.requireSecret) {
        val savedSecret =
          skillManagerViewModel.dataStoreRepository.readSecret(
            key = getSkillSecretKey(skillName = skillName)
          )
        if (savedSecret == null || savedSecret.isEmpty()) {
          val action =
            AskInfoAgentAction(
              dialogTitle = "Enter secret",
              fieldLabel =
                skill.requireSecretDescription.ifEmpty {
                  "The JS script needs a secret (API key / token) to proceed:"
                },
            )
          _actionChannel.send(action)
          secret = action.result.await()
          if (secret.isNotEmpty()) {
            skillManagerViewModel.dataStoreRepository.saveSecret(
              key = getSkillSecretKey(skillName = skillName),
              value = secret,
            )
            Log.d(TAG, "Got Secret from ask info dialog: ${secret.substring(0, 3)}")
          } else {
            Log.d(TAG, "The ask info dialog got cancelled. No secret.")
          }
        } else {
          secret = savedSecret
        }
      }

      // Get the url for the skill.
      val url =
        skillManagerViewModel.getJsSkillUrl(skillName = skillName, scriptName = scriptName)
          ?: return@runBlocking mapOf(
            "result" to "JS Skill URL not set properly or skill not found"
          )
      Log.d(TAG, "Calling JS script.\n- url: $url\n- data: $data")

      // Update progress.
      _actionChannel.send(
        SkillProgressAgentAction(
          label = "Calling JS script \"${skillName}/${scriptName}\"",
          inProgress = true,
          addItemTitle = "Call JS script: \"${skillName}/${scriptName}\"",
          addItemDescription = "- URL: ${url.replace(LOCAL_URL_BASE, "")}\n- Data: $data",
          customData = skill,
        )
      )

      // Actually run it and wait for the result.
      val action =
        CallJsAgentAction(url = url, data = data.trim().ifEmpty { "{}" }, secret = secret)
      _actionChannel.send(action)
      val result = action.result.await()

      // Try to parse result to CallJsSkillResult.
      val moshi: Moshi = Moshi.Builder().build()
      val jsonAdapter: JsonAdapter<CallJsSkillResult> =
        moshi.adapter(CallJsSkillResult::class.java).failOnUnknown()
      val resultJson = runCatching { jsonAdapter.fromJson(result) }.getOrNull()
      val error = resultJson?.error

      // Failed to parse. Treat its whole as a result string.
      if (
        resultJson == null ||
          (resultJson.result == null && resultJson.webview == null && resultJson.image == null)
      ) {
        mapOf("result" to result, "status" to "succeeded")
      }
      // Error case.
      else if (error != null) {
        mapOf("error" to error, "status" to "failed")
      }
      // Non-error cases.
      else {
        // Handle image and webview in result.
        val image = resultJson.image
        val webview = resultJson.webview
        if (image != null) {
          Log.d(TAG, "Got an image response.")
          resultImageToShow = image
        }
        if (webview != null) {
          Log.d(TAG, "Got an webview response.")
          val webviewUrl =
            skillManagerViewModel.getJsSkillWebviewUrl(
              skillName = skillName,
              url = webview.url ?: "",
            )
          Log.d(TAG, "Webview url: $webviewUrl")
          resultWebviewToShow = webview.copy(url = webviewUrl)
        }
        Log.d(TAG, "Result: ${resultJson.result}")
        mapOf("result" to (resultJson.result ?: ""), "status" to "succeeded")
      }
    }
  }

  @Tool(
    description =
      "Run an Android intent. It is used to interact with the app to perform certain actions."
  )
  fun runIntent(
    @ToolParam(description = "The intent to run.") intent: String,
    @ToolParam(
      description = "A JSON string containing the parameter values required for the intent."
    )
    parameters: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      Log.d(TAG, "Run intent. Intent: '$intent', parameters: '$parameters'")
      _actionChannel.send(
        SkillProgressAgentAction(
          label = "Executing intent \"$intent\"",
          inProgress = true,
          addItemTitle = "Execute intent \"$intent\"",
          addItemDescription = "Parameters: $parameters",
        )
      )
      if (IntentHandler.handleAction(context, intent, parameters)) {
        return@runBlocking mapOf(
          "action" to intent,
          "parameters" to parameters,
          "result" to "succeeded",
        )
      } else {
        return@runBlocking mapOf(
          "action" to intent,
          "parameters" to parameters,
          "result" to "failed",
        )
      }
    }
  }

  /** Read contacts from the phone. Permission must be granted before calling. */
  @Tool(description = "Reads contacts from the phone. Requires READ_CONTACTS permission.")
  fun readContacts(
    @ToolParam(description = "Search query to filter contacts by name. Use empty string to get all contacts.")
    query: String
  ): Map<String, Any> {
    return runBlocking(Dispatchers.Default) {
      try {
        val action = RequestPermissionAgentAction(android.Manifest.permission.READ_CONTACTS)
        _actionChannel.send(action)
        val granted = action.result.await()
        if (!granted) {
          return@runBlocking mapOf("error" to "READ_CONTACTS permission denied", "contacts" to emptyList<Any>(), "count" to 0)
        }

        val contacts = mutableListOf<Map<String, String>>()
        val selection = if (query.isBlank()) null else "${android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"
        val selectionArgs = if (query.isBlank()) null else arrayOf("%$query%")
        val cursor = context.contentResolver.query(
          android.provider.ContactsContract.Contacts.CONTENT_URI,
          arrayOf(
            android.provider.ContactsContract.Contacts._ID,
            android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
          ),
          selection,
          selectionArgs,
          "${android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )
        cursor?.use { c ->
          val idCol = c.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
          val nameCol = c.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
          while (c.moveToNext() && contacts.size < 20) {
            val id = c.getString(idCol) ?: continue
            val name = c.getString(nameCol) ?: continue
            // Get phone numbers for this contact
            val phones = mutableListOf<String>()
            val phoneCursor = context.contentResolver.query(
              android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
              arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
              "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
              arrayOf(id),
              null
            )
            phoneCursor?.use { pc ->
              val numCol = pc.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
              while (pc.moveToNext()) {
                pc.getString(numCol)?.let { phones.add(it) }
              }
            }
            contacts.add(mapOf("name" to name, "phone" to phones.firstOrNull().orEmpty()))
          }
        }
        mapOf("contacts" to contacts, "count" to contacts.size)
      } catch (e: Exception) {
        mapOf("error" to "Failed to read contacts: ${e.message}", "contacts" to emptyList<Any>(), "count" to 0)
      }
    }
  }

  /** Read calendar events from the phone. Permission must be granted before calling. */
  @Tool(description = "Reads upcoming calendar events from the phone.")
  fun readCalendar(
    @ToolParam(description = "Number of days from today to look ahead (e.g. '7' for one week)")
    daysAhead: String
  ): Map<String, Any> {
    return runBlocking(Dispatchers.Default) {
      try {
        val action = RequestPermissionAgentAction(android.Manifest.permission.READ_CALENDAR)
        _actionChannel.send(action)
        val granted = action.result.await()
        if (!granted) {
          return@runBlocking mapOf("error" to "READ_CALENDAR permission denied", "events" to emptyList<Any>())
        }

        val days = daysAhead.toIntOrNull()?.coerceIn(1, 30) ?: 7
        val now = System.currentTimeMillis()
        val end = now + (days.toLong() * 24 * 60 * 60 * 1000)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())

        val events = mutableListOf<Map<String, String>>()
        val cursor = context.contentResolver.query(
          android.provider.CalendarContract.Events.CONTENT_URI,
          arrayOf(
            android.provider.CalendarContract.Events.TITLE,
            android.provider.CalendarContract.Events.DTSTART,
            android.provider.CalendarContract.Events.DTEND,
            android.provider.CalendarContract.Events.EVENT_LOCATION,
            android.provider.CalendarContract.Events.DESCRIPTION,
          ),
          "${android.provider.CalendarContract.Events.DTSTART} >= ? AND ${android.provider.CalendarContract.Events.DTSTART} <= ? AND ${android.provider.CalendarContract.Events.DELETED} = 0",
          arrayOf(now.toString(), end.toString()),
          "${android.provider.CalendarContract.Events.DTSTART} ASC"
        )
        cursor?.use { c ->
          val titleCol = c.getColumnIndex(android.provider.CalendarContract.Events.TITLE)
          val startCol = c.getColumnIndex(android.provider.CalendarContract.Events.DTSTART)
          val endCol = c.getColumnIndex(android.provider.CalendarContract.Events.DTEND)
          val locCol = c.getColumnIndex(android.provider.CalendarContract.Events.EVENT_LOCATION)
          while (c.moveToNext() && events.size < 20) {
            val event = mutableMapOf<String, String>()
            event["title"] = c.getString(titleCol) ?: "Untitled"
            val startMs = c.getLong(startCol)
            val endMs = c.getLong(endCol)
            event["start"] = sdf.format(java.util.Date(startMs))
            event["end"] = sdf.format(java.util.Date(endMs))
            val loc = c.getString(locCol)
            if (!loc.isNullOrBlank()) event["location"] = loc
            events.add(event)
          }
        }
        mapOf("events" to events, "count" to events.size)
      } catch (e: Exception) {
        mapOf("error" to "Failed to read calendar: ${e.message}", "events" to emptyList<Any>())
      }
    }
  }

  /** Fetch text content from a web URL. */
  @Tool(description = "Fetches the text content of a web page from a URL.")
  fun fetchUrl(
    @ToolParam(description = "The full URL to fetch (must start with http:// or https://)")
    url: String
  ): Map<String, Any> {
    return runBlocking(Dispatchers.IO) {
      try {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
          return@runBlocking mapOf("error" to "URL must start with http:// or https://")
        }
        _actionChannel.send(
          SkillProgressAgentAction(
            label = "Fetching URL: $url",
            inProgress = true,
            addItemTitle = "Fetch URL",
            addItemDescription = url,
          )
        )
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
          return@runBlocking mapOf("error" to "HTTP $responseCode", "url" to url)
        }
        val html = connection.inputStream.bufferedReader().readText()
        // Simple HTML to text: strip tags
        val titleMatch = Regex("<title[^>]*>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
        val title = titleMatch?.groupValues?.get(1)?.trim() ?: ""
        val noScript = html.replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), " ")
        val noStyle = noScript.replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), " ")
        val text = noStyle.replace(Regex("<[^>]+>"), " ")
          .replace(Regex("\\s+"), " ")
          .trim()
          .take(4000)
        mapOf("title" to title, "content" to text, "url" to url)
      } catch (e: Exception) {
        mapOf("error" to "Failed to fetch URL: ${e.message}", "url" to url)
      }
    }
  }

  fun sendAgentAction(action: AgentAction) {
    runBlocking(Dispatchers.Default) { _actionChannel.send(action) }
  }
}

fun getSkillSecretKey(skillName: String): String {
  return "skill___${skillName}"
}
