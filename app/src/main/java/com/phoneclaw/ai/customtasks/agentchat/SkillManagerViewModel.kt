package com.phoneclaw.ai.customtasks.agentchat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneclaw.ai.common.LOCAL_URL_BASE
import com.phoneclaw.ai.common.SkillTryOutChip
import com.phoneclaw.ai.common.getJsonResponse
import com.phoneclaw.ai.data.DataStoreRepository
import com.phoneclaw.ai.proto.Skill
import com.google.ai.edge.litertlm.Contents
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import javax.inject.Inject
import kotlin.collections.joinToString
import kotlin.io.encoding.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AGSkillManagerVM"

private const val SKILL_ALLOWLIST_URL = ""

val TRYOUT_CHIPS: List<SkillTryOutChip> =
  listOf(
    SkillTryOutChip(
      icon = Icons.Outlined.Map,
      label = "Interactive Map",
      prompt = "Show me Googleplex on interactive map.",
      skillName = "interactive-map",
    ),
    SkillTryOutChip(
      icon = Icons.Outlined.LocalLibrary,
      label = "Query Wikipedia",
      prompt = "Check Wikipedia about Oscars 2026. Tell me who won the best picture.",
      skillName = "query-wikipedia",
    ),
    SkillTryOutChip(
      icon = Icons.Outlined.Contacts,
      label = "Read Contacts",
      prompt = "Show me my contacts.",
      skillName = "phone-contacts",
    ),
    SkillTryOutChip(
      icon = Icons.Outlined.CalendarToday,
      label = "My Calendar",
      prompt = "What events do I have this week?",
      skillName = "calendar-manager",
    ),
    SkillTryOutChip(
      icon = Icons.Outlined.Alarm,
      label = "Set Alarm",
      prompt = "Set an alarm for 7:30 AM tomorrow.",
      skillName = "alarm-clock",
    ),
    SkillTryOutChip(
      icon = Icons.Outlined.Apps,
      label = "Open App",
      prompt = "Open YouTube.",
      skillName = "app-launcher",
    ),
    SkillTryOutChip(
      icon = Icons.Outlined.Language,
      label = "Browse Link",
      prompt = "What is on https://en.wikipedia.org/wiki/Kotlin_(programming_language) ? Give me a summary.",
      skillName = "web-browser",
    ),
  )

data class SkillState(val skill: Skill)

data class SkillManagerUiState(
  val loading: Boolean = false,
  val skills: List<SkillState> = listOf(),
  val validating: Boolean = false,
  val validationError: String? = null,
)

@Singleton
class SkillManagerViewModel
@Inject
constructor(
  val dataStoreRepository: DataStoreRepository,
  @ApplicationContext private val context: Context,
) : ViewModel() {
  private val _uiState = MutableStateFlow(SkillManagerUiState())
  val uiState = _uiState.asStateFlow()
  var skillLoaded = false

  fun loadSkills(onDone: () -> Unit) {
    if (!skillLoaded) {
      setLoading(true)
      viewModelScope.launch(Dispatchers.IO) {
        Log.d(TAG, "Loading skills index...")

        val allDataStoreSkills = dataStoreRepository.getAllSkills()
        val builtInSelectionMap = allDataStoreSkills.filter { it.builtIn }.associate { it.name to it.selected }

        val builtInSkills = mutableListOf<Skill>()
        try {
          val skillAssetDirs = context.assets.list("skills") ?: emptyArray()
          for (dirName in skillAssetDirs) {
            val skillMdPath = "skills/$dirName/SKILL.md"
            try {
              context.assets.open(skillMdPath).use { inputStream ->
                val mdContent = inputStream.bufferedReader().use { it.readText() }
                val (skillProto, errors) =
                  convertSkillMdToProto(
                    mdContent,
                    builtIn = true,
                    selected = true,
                    importDir = "assets/skills/$dirName",
                  )
                if (errors.isEmpty()) {
                  skillProto?.let {
                    val selectedState = builtInSelectionMap[it.name] ?: true
                    builtInSkills.add(it.toBuilder().setSelected(selectedState).build())
                  }
                }
              }
            } catch (e: Exception) {
              Log.w(TAG, "Error reading asset skill $dirName", e)
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error listing assets/skills", e)
        }

        val finalSkills = builtInSkills.toMutableList()
        for (customSkill in allDataStoreSkills.filter { !it.builtIn }) {
          if (!finalSkills.any { it.name == customSkill.name }) {
            finalSkills.add(customSkill)
          }
        }

        dataStoreRepository.setSkills(finalSkills)

        _uiState.update { currentState ->
          currentState.copy(skills = finalSkills.map { SkillState(skill = it) })
        }

        setLoading(false)
        skillLoaded = true
        withContext(Dispatchers.Default) { onDone() }
      }
    } else {
      onDone()
    }
  }

  fun isSkillSelected(skillName: String): Boolean {
    return _uiState.value.skills.firstOrNull { it.skill.name == skillName }?.skill?.selected == true
  }

  fun getSelectedSkills(): List<Skill> {
    return _uiState.value.skills.filter { it.skill.selected }.map { it.skill }
  }

  fun getSystemPrompt(baseSystemPrompt: String): Contents {
    val selectedSkillsNamesAndDescriptions = getSelectedSkillsNamesAndDescriptions()
    val systemPrompt = baseSystemPrompt.replace("___SKILLS___", selectedSkillsNamesAndDescriptions)
    return Contents.of(systemPrompt)
  }

  fun getSkill(name: String): Skill? {
    return _uiState.value.skills.firstOrNull { it.skill.name == name }?.skill
  }

  fun getJsSkillUrl(skillName: String, scriptName: String): String? {
    val skill = getSkill(name = skillName) ?: return null
    var baseUrl = ""
    if (skill.importDirName.isNotEmpty()) {
      baseUrl = "$LOCAL_URL_BASE/${skill.importDirName}"
    } else if (skill.skillUrl.isNotEmpty()) {
      baseUrl = skill.skillUrl
    }
    if (baseUrl.isEmpty()) return null
    return "$baseUrl/scripts/$scriptName"
  }

  fun getJsSkillWebviewUrl(skillName: String, url: String): String {
    val skill = getSkill(name = skillName) ?: return url
    if (url.startsWith("http")) return url
    var baseUrl = ""
    if (skill.importDirName.isNotEmpty()) {
      baseUrl = "$LOCAL_URL_BASE/${skill.importDirName}"
    } else if (skill.skillUrl.isNotEmpty()) {
      baseUrl = skill.skillUrl
    }
    if (baseUrl.isEmpty()) return url
    return "$baseUrl/assets/$url"
  }

  fun getSelectedSkillsNamesAndDescriptions(): String {
    return this.getSelectedSkills().joinToString("\n") { skill ->
      "- ${skill.name}: ${skill.description}"
    }
  }

  fun convertSkillMdToProto(
    mdContent: String,
    builtIn: Boolean,
    selected: Boolean,
    skillUrl: String = "",
    importDir: String = "",
  ): Pair<Skill?, List<String>> {
    val parts = mdContent.split("---")
    val errors = mutableListOf<String>()
    if (parts.size < 3) {
      errors.add("Invalid format")
      return Pair(null, errors)
    }
    val header = parts[1].trim()
    var name: String? = null
    var description: String? = null
    var requireSecret = false
    var requireSecretDescription = ""
    var homepage: String? = null

    var startMetadata = false
    for (line in header.lines()) {
      val trimmedLine = line.trim()
      if (trimmedLine == "metadata:") {
        startMetadata = true
        continue
      }
      if (!startMetadata) {
        when {
          trimmedLine.startsWith("name:") -> name = trimmedLine.substringAfter("name:").trim()
          trimmedLine.startsWith("description:") -> description = trimmedLine.substringAfter("description:").trim()
        }
      } else {
        when {
          trimmedLine.startsWith("require-secret:") -> requireSecret = trimmedLine.substringAfter("require-secret:").trim().toBoolean()
          trimmedLine.startsWith("require-secret-description:") -> requireSecretDescription = trimmedLine.substringAfter("require-secret-description:").trim()
          trimmedLine.startsWith("homepage:") -> homepage = trimmedLine.substringAfter("homepage:").trim()
        }
      }
    }

    if (name.isNullOrEmpty()) errors.add("Missing name")
    if (description.isNullOrEmpty()) errors.add("Missing description")
    val instructions = parts.drop(2).joinToString("---").trim()
    if (errors.isNotEmpty()) return Pair(null, errors)

    val skill = Skill.newBuilder()
        .setName(name!!)
        .setDescription(description!!)
        .setInstructions(instructions)
        .setBuiltIn(builtIn)
        .setSelected(selected)
        .setSkillUrl(skillUrl)
        .setRequireSecret(requireSecret)
        .setRequireSecretDescription(requireSecretDescription)
        .setHomepage(homepage ?: "")
        .setImportDirName(importDir)
        .build()

    return Pair(skill, emptyList())
  }

  fun setLoading(loading: Boolean) {
    _uiState.update { it.copy(loading = loading) }
  }

  fun setSkillSelected(skill: SkillState, selected: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStoreRepository.setSkillSelected(skill.skill, selected)
      _uiState.update { currentState ->
        currentState.copy(
          skills = currentState.skills.map {
            if (it.skill.name == skill.skill.name) {
              SkillState(skill = it.skill.toBuilder().setSelected(selected).build())
            } else it
          }
        )
      }
    }
  }

  fun setAllSkillsSelected(selected: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      val currentSkills = _uiState.value.skills
      currentSkills.forEach { skillState ->
        dataStoreRepository.setSkillSelected(skillState.skill, selected)
      }
      _uiState.update { currentState ->
        currentState.copy(
          skills = currentState.skills.map { skillState ->
            SkillState(skill = skillState.skill.toBuilder().setSelected(selected).build())
          }
        )
      }
    }
  }

  fun deleteSkill(name: String) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStoreRepository.deleteSkill(name)
      _uiState.update { currentState ->
        currentState.copy(skills = currentState.skills.filter { it.skill.name != name })
      }
    }
  }

  fun deleteSkills(names: Set<String>) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStoreRepository.deleteSkills(names)
      _uiState.update { currentState ->
        currentState.copy(skills = currentState.skills.filter { it.skill.name !in names })
      }
    }
  }

  fun addSkill(skill: Skill, addToDataStore: Boolean = true) {
    viewModelScope.launch(Dispatchers.IO) {
      if (addToDataStore) {
        dataStoreRepository.addSkill(skill)
      }
      _uiState.update { currentState ->
        if (currentState.skills.none { it.skill.name == skill.name }) {
          currentState.copy(skills = currentState.skills + SkillState(skill = skill))
        } else {
          currentState
        }
      }
    }
  }

  fun setValidationError(error: String?) {
    _uiState.update { it.copy(validationError = error) }
  }

  fun importSkillFromUrl(url: String, onDone: (Skill?, String?) -> Unit) {
    _uiState.update { it.copy(validating = true, validationError = null) }
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val mdContent = java.net.URL(url).readText()
        val (skill, errors) = convertSkillMdToProto(
          mdContent = mdContent,
          builtIn = false,
          selected = true,
          skillUrl = url.substringBeforeLast("/"),
        )
        if (errors.isNotEmpty() || skill == null) {
          val errorMsg = errors.joinToString(", ")
          _uiState.update { it.copy(validating = false, validationError = errorMsg) }
          withContext(Dispatchers.Main) { onDone(null, errorMsg) }
        } else {
          addSkill(skill)
          _uiState.update { it.copy(validating = false, validationError = null) }
          withContext(Dispatchers.Main) { onDone(skill, null) }
        }
      } catch (e: Exception) {
        val errorMsg = "Failed to fetch URL: ${e.message}"
        _uiState.update { it.copy(validating = false, validationError = errorMsg) }
        withContext(Dispatchers.Main) { onDone(null, errorMsg) }
      }
    }
  }

  fun importSkillFromLocalUri(uri: Uri, onDone: (Skill?, String?) -> Unit) {
    _uiState.update { it.copy(validating = true, validationError = null) }
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val mdContent = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
          ?: run {
            val errorMsg = "Failed to read file"
            _uiState.update { it.copy(validating = false, validationError = errorMsg) }
            withContext(Dispatchers.Main) { onDone(null, errorMsg) }
            return@launch
          }
        val (skill, errors) = convertSkillMdToProto(
          mdContent = mdContent,
          builtIn = false,
          selected = true,
        )
        if (errors.isNotEmpty() || skill == null) {
          val errorMsg = errors.joinToString(", ")
          _uiState.update { it.copy(validating = false, validationError = errorMsg) }
          withContext(Dispatchers.Main) { onDone(null, errorMsg) }
        } else {
          addSkill(skill)
          _uiState.update { it.copy(validating = false, validationError = null) }
          withContext(Dispatchers.Main) { onDone(skill, null) }
        }
      } catch (e: Exception) {
        val errorMsg = "Failed to import skill: ${e.message}"
        _uiState.update { it.copy(validating = false, validationError = errorMsg) }
        withContext(Dispatchers.Main) { onDone(null, errorMsg) }
      }
    }
  }
}

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
fun decodeBase64ToBitmap(base64String: String): Bitmap? {
  return try {
    val pureBase64 = base64String.substringAfter(",")
    val imageBytes = Base64.decode(pureBase64)
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
  } catch (e: Exception) {
    null
  }
}
