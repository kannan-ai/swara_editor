package com.example.swara_editor.videoeditor.data

import android.content.Context
import android.net.Uri
import com.example.swara_editor.videoeditor.models.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProjectRepository(private val context: Context) {

    private val projectsDir: File
        get() {
            val dir = File(context.filesDir, "swara_projects")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun saveProject(project: VideoProject) {
        try {
            val file = File(projectsDir, "${project.id}.json")
            val json = JSONObject().apply {
                put("id", project.id)
                put("name", project.name)
                put("durationMs", project.durationMs)
                put("coverUri", project.coverUri?.toString() ?: "")

                val assetsArr = JSONArray()
                project.assets.forEach { asset ->
                    assetsArr.put(JSONObject().apply {
                        put("id", asset.id)
                        put("uri", asset.uri.toString())
                        put("name", asset.name)
                        put("type", asset.type.name)
                        put("durationMs", asset.durationMs)
                        put("width", asset.width)
                        put("height", asset.height)
                    })
                }
                put("assets", assetsArr)

                val layersArr = JSONArray()
                project.layers.forEach { layer ->
                    layersArr.put(JSONObject().apply {
                        put("id", layer.id)
                        put("assetId", layer.assetId)
                        put("startTimeMs", layer.startTimeMs)
                        put("durationMs", layer.durationMs)
                        put("startOffsetMs", layer.startOffsetMs)
                        put("volume", layer.volume.toDouble())
                        put("speed", layer.speed.toDouble())
                        put("scale", layer.scale.toDouble())
                        put("rotation", layer.rotation.toDouble())
                        put("positionX", layer.positionX.toDouble())
                        put("positionY", layer.positionY.toDouble())
                        put("fadeInMs", layer.fadeInMs)
                        put("fadeOutMs", layer.fadeOutMs)
                    })
                }
                put("layers", layersArr)

                val textArr = JSONArray()
                project.textOverlays.forEach { text ->
                    textArr.put(JSONObject().apply {
                        put("id", text.id)
                        put("text", text.text)
                        put("startTimeMs", text.startTimeMs)
                        put("durationMs", text.durationMs)
                        put("stylePreset", text.stylePreset.name)
                    })
                }
                put("textOverlays", textArr)
            }
            file.writeText(json.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAllProjects(): List<VideoProject> {
        val projects = mutableListOf<VideoProject>()
        try {
            val files = projectsDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
            files.forEach { file ->
                runCatching {
                    val jsonObj = JSONObject(file.readText())
                    val proj = parseProject(jsonObj)
                    projects.add(proj)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return projects.sortedByDescending { it.id }
    }

    fun loadProject(id: String): VideoProject? {
        return try {
            val file = File(projectsDir, "$id.json")
            if (file.exists()) {
                parseProject(JSONObject(file.readText()))
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun renameProject(id: String, newName: String) {
        val project = loadProject(id)
        if (project != null) {
            saveProject(project.copy(name = newName))
        }
    }

    fun deleteProject(id: String) {
        try {
            val file = File(projectsDir, "$id.json")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseProject(json: JSONObject): VideoProject {
        val id = json.optString("id", "")
        val name = json.optString("name", "Untitled")
        val durationMs = json.optLong("durationMs", 0L)
        val coverUriStr = json.optString("coverUri", "")
        val coverUri = if (coverUriStr.isNotBlank()) Uri.parse(coverUriStr) else null

        val assetsList = mutableListOf<ProjectAsset>()
        val assetsArr = json.optJSONArray("assets") ?: JSONArray()
        for (i in 0 until assetsArr.length()) {
            val aObj = assetsArr.getJSONObject(i)
            assetsList.add(
                ProjectAsset(
                    id = aObj.optString("id", ""),
                    uri = Uri.parse(aObj.optString("uri", "")),
                    name = aObj.optString("name", "Asset"),
                    type = runCatching { AssetType.valueOf(aObj.optString("type", "VIDEO")) }.getOrDefault(AssetType.VIDEO),
                    durationMs = aObj.optLong("durationMs", 0L),
                    width = aObj.optInt("width", 1920),
                    height = aObj.optInt("height", 1080)
                )
            )
        }

        val layersList = mutableListOf<VideoLayer>()
        val layersArr = json.optJSONArray("layers") ?: JSONArray()
        for (i in 0 until layersArr.length()) {
            val lObj = layersArr.getJSONObject(i)
            layersList.add(
                VideoLayer(
                    id = lObj.optString("id", ""),
                    assetId = lObj.optString("assetId", ""),
                    startTimeMs = lObj.optLong("startTimeMs", 0L),
                    durationMs = lObj.optLong("durationMs", 0L),
                    startOffsetMs = lObj.optLong("startOffsetMs", 0L),
                    volume = lObj.optDouble("volume", 1.0).toFloat(),
                    speed = lObj.optDouble("speed", 1.0).toFloat(),
                    scale = lObj.optDouble("scale", 1.0).toFloat(),
                    rotation = lObj.optDouble("rotation", 0.0).toFloat(),
                    positionX = lObj.optDouble("positionX", 0.5).toFloat(),
                    positionY = lObj.optDouble("positionY", 0.5).toFloat(),
                    fadeInMs = lObj.optLong("fadeInMs", 0L),
                    fadeOutMs = lObj.optLong("fadeOutMs", 0L)
                )
            )
        }

        val textList = mutableListOf<TimedTextOverlay>()
        val textArr = json.optJSONArray("textOverlays") ?: JSONArray()
        for (i in 0 until textArr.length()) {
            val tObj = textArr.getJSONObject(i)
            textList.add(
                TimedTextOverlay(
                    id = tObj.optString("id", ""),
                    text = tObj.optString("text", ""),
                    startTimeMs = tObj.optLong("startTimeMs", 0L),
                    durationMs = tObj.optLong("durationMs", 1000L),
                    stylePreset = runCatching { TextWordArtStyle.valueOf(tObj.optString("stylePreset", "CLASSIC_SHADOW")) }.getOrDefault(TextWordArtStyle.CLASSIC_SHADOW)
                )
            )
        }

        return VideoProject(
            id = id,
            name = name,
            assets = assetsList,
            layers = layersList,
            textOverlays = textList,
            durationMs = durationMs,
            coverUri = coverUri
        )
    }
}
