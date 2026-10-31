package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.local.entity.Match
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.data.repository.TheBiGRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class ConflictResolution {
    OVERWRITE,
    CREATE_COPY
}

data class ConflictCheckResult(
    val hasConflict: Boolean,
    val duplicateTeamNames: List<String> = emptyList(),
    val duplicateTournamentNames: List<String> = emptyList()
)

object TheBiGBackupHelper {

    /**
     * Checks if the device has an active internet or local network connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_WIFI_P2P)
    }

    /**
     * Generates an Android Bitmap QR Code using ZXing.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Exports a tournament to a proprietary .tb ZIP container:
     * Contains data.json + crests/ folder with PNG crest images.
     */
    suspend fun exportTournamentTbFile(
        context: Context,
        repository: TheBiGRepository,
        tournamentId: Long
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("format", "THEBIG_TB_ZIP")
            root.put("version", 7)
            root.put("type", "TOURNAMENT")
            root.put("timestamp", System.currentTimeMillis())

            val tournament = repository.getTournamentOnce(tournamentId) ?: return@withContext null
            val tObj = JSONObject().apply {
                put("id", tournament.id)
                put("name", tournament.name)
                put("edition", tournament.edition)
                put("entryFee", tournament.entryFee)
                put("playersPerTeam", tournament.playersPerTeam)
                put("matchDurationMinutes", tournament.matchDurationMinutes)
                put("status", tournament.status)
                put("tournamentType", tournament.tournamentType)
                put("awardTiming", tournament.awardTiming)
                put("financialModel", tournament.financialModel)
                put("prizeChampionAmount", tournament.prizeChampionAmount)
                put("prizeRunnerUpAmount", tournament.prizeRunnerUpAmount)
                put("prizeTopScorerAmount", tournament.prizeTopScorerAmount)
                put("prizeBestGkAmount", tournament.prizeBestGkAmount)
            }
            root.put("tournament", tObj)

            val teams = repository.getTeamsForTournamentOnce(tournamentId)
            val teamArr = JSONArray()
            val crestsToZip = mutableListOf<Pair<String, File>>() // zipEntryName -> File

            for (team in teams) {
                val obj = JSONObject().apply {
                    put("id", team.id)
                    put("name", team.name)
                    put("shortName", team.shortName)
                    put("shieldVectorIndex", team.shieldVectorIndex)
                    put("shieldPrimaryColorHex", team.shieldPrimaryColorHex)
                    put("primaryColorHex", team.primaryColorHex)
                    put("secondaryColorHex", team.secondaryColorHex)
                    put("borderColorHex", team.borderColorHex)
                    put("jerseyPattern", team.jerseyPattern)
                    put("goalkeeperColorHex", team.goalkeeperColorHex)

                    if (team.customShieldUri != null) {
                        try {
                            val uri = Uri.parse(team.customShieldUri)
                            val path = uri.path
                            if (path != null) {
                                val file = File(path)
                                if (file.exists()) {
                                    val entryName = "crests/shield_team_${team.id}.png"
                                    put("crestZipEntry", entryName)
                                    crestsToZip.add(entryName to file)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                teamArr.put(obj)
            }
            root.put("teams", teamArr)

            val matches = repository.getMatchesForTournamentOnce(tournamentId)
            val matchesArr = JSONArray()
            for (m in matches) {
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("homeTeamId", m.homeTeamId)
                    put("awayTeamId", m.awayTeamId)
                    put("homeScore", m.homeScore)
                    put("awayScore", m.awayScore)
                    put("homePenalties", m.homePenalties)
                    put("awayPenalties", m.awayPenalties)
                    put("roundName", m.roundName)
                    put("status", m.status)
                }
                matchesArr.put(obj)
            }
            root.put("matches", matchesArr)

            val sanitizedName = tournament.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val fileName = "Torneo_${sanitizedName}_TheBiG.tb"

            val exportDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
            val zipFile = File(exportDir, fileName)

            writeTbZipFile(zipFile, root.toString(2), crestsToZip)

            FileProvider.getUriForFile(
                context,
                "com.aistudio.thebig.ftbol.fileprovider",
                zipFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports full database to dedicated .tb ZIP container.
     */
    suspend fun exportFullDatabaseTbFile(
        context: Context,
        repository: TheBiGRepository
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val jsonContent = repository.exportDatabaseToJson()
            val root = JSONObject(jsonContent)
            root.put("format", "THEBIG_TB_ZIP")
            root.put("version", 7)
            root.put("type", "FULL")

            val allTeams = repository.getAllTeamsOnce()
            val crestsToZip = mutableListOf<Pair<String, File>>()

            for (team in allTeams) {
                if (team.customShieldUri != null) {
                    try {
                        val uri = Uri.parse(team.customShieldUri)
                        val path = uri.path
                        if (path != null) {
                            val file = File(path)
                            if (file.exists()) {
                                val entryName = "crests/shield_team_${team.id}.png"
                                crestsToZip.add(entryName to file)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val fileName = "TheBiG_Full_Backup.tb"
            val exportDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
            val zipFile = File(exportDir, fileName)

            writeTbZipFile(zipFile, root.toString(2), crestsToZip)

            FileProvider.getUriForFile(
                context,
                "com.aistudio.thebig.ftbol.fileprovider",
                zipFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports a single team roster and crest to .tb file.
     */
    suspend fun exportSingleTeamTbFile(
        context: Context,
        repository: TheBiGRepository,
        teamId: Long
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val allTeams = repository.getAllTeamsOnce()
            val team = allTeams.find { tm -> tm.id == teamId } ?: return@withContext null
            val players = repository.getPlayersForTeamOnce(teamId)

            val root = JSONObject()
            root.put("format", "THEBIG_TB_ZIP")
            root.put("version", 7)
            root.put("type", "SINGLE_TEAM")
            root.put("timestamp", System.currentTimeMillis())

            val teamObj = JSONObject().apply {
                put("id", team.id)
                put("name", team.name)
                put("shortName", team.shortName)
                put("shieldVectorIndex", team.shieldVectorIndex)
                put("shieldPrimaryColorHex", team.shieldPrimaryColorHex)
                put("primaryColorHex", team.primaryColorHex)
                put("secondaryColorHex", team.secondaryColorHex)
                put("borderColorHex", team.borderColorHex)
                put("jerseyPattern", team.jerseyPattern)
                put("goalkeeperColorHex", team.goalkeeperColorHex)
            }
            root.put("team", teamObj)

            val playersArr = JSONArray()
            for (p in players) {
                val pObj = JSONObject().apply {
                    put("name", p.name)
                    put("dorsal", p.dorsal)
                    put("isGoalkeeper", p.isGoalkeeper)
                    put("isCaptain", p.isCaptain)
                }
                playersArr.put(pObj)
            }
            root.put("players", playersArr)

            val crestsToZip = mutableListOf<Pair<String, File>>()
            if (team.customShieldUri != null) {
                try {
                    val uri = Uri.parse(team.customShieldUri)
                    val path = uri.path
                    if (path != null) {
                        val file = File(path)
                        if (file.exists()) {
                            crestsToZip.add("crests/shield_team_${team.id}.png" to file)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val sanitizedName = team.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val fileName = "Club_${sanitizedName}_TheBiG.tb"
            val exportDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
            val zipFile = File(exportDir, fileName)

            writeTbZipFile(zipFile, root.toString(2), crestsToZip)

            FileProvider.getUriForFile(
                context,
                "com.aistudio.thebig.ftbol.fileprovider",
                zipFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeTbZipFile(
        destFile: File,
        jsonContent: String,
        crests: List<Pair<String, File>>
    ) {
        ZipOutputStream(FileOutputStream(destFile)).use { zos ->
            // Entry 1: data.json
            val jsonEntry = ZipEntry("data.json")
            zos.putNextEntry(jsonEntry)
            zos.write(jsonContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Entries in crests/
            for ((entryName, file) in crests) {
                if (file.exists()) {
                    val crestEntry = ZipEntry(entryName)
                    zos.putNextEntry(crestEntry)
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    /**
     * Checks if a .tb raw content string or byte array contains conflicting team or tournament names.
     */
    suspend fun checkConflicts(
        repository: TheBiGRepository,
        tbContent: String
    ): ConflictCheckResult = withContext(Dispatchers.IO) {
        try {
            val jsonStr = extractJsonFromTbContent(tbContent)
            val root = JSONObject(jsonStr)

            val existingTeams = repository.getAllTeamsOnce().map { tm -> tm.name.trim().lowercase() }.toSet()
            val existingTourneys = repository.getAllTournamentsOnce().map { tr -> tr.name.trim().lowercase() }.toSet()

            val dupTeams = mutableListOf<String>()
            val dupTourneys = mutableListOf<String>()

            // Check tournament
            if (root.has("tournament")) {
                val tName = root.getJSONObject("tournament").getString("name")
                if (existingTourneys.contains(tName.trim().lowercase())) {
                    dupTourneys.add(tName)
                }
            }
            if (root.has("tournaments")) {
                val tArr = root.getJSONArray("tournaments")
                for (i in 0 until tArr.length()) {
                    val tName = tArr.getJSONObject(i).getString("name")
                    if (existingTourneys.contains(tName.trim().lowercase())) {
                        dupTourneys.add(tName)
                    }
                }
            }

            // Check teams
            if (root.has("team")) {
                val tName = root.getJSONObject("team").getString("name")
                if (existingTeams.contains(tName.trim().lowercase())) {
                    dupTeams.add(tName)
                }
            }
            if (root.has("teams")) {
                val tArr = root.getJSONArray("teams")
                for (i in 0 until tArr.length()) {
                    val tName = tArr.getJSONObject(i).getString("name")
                    if (existingTeams.contains(tName.trim().lowercase())) {
                        dupTeams.add(tName)
                    }
                }
            }

            ConflictCheckResult(
                hasConflict = dupTeams.isNotEmpty() || dupTourneys.isNotEmpty(),
                duplicateTeamNames = dupTeams,
                duplicateTournamentNames = dupTourneys
            )
        } catch (e: Exception) {
            ConflictCheckResult(hasConflict = false)
        }
    }

    /**
     * Extracts json content either from a plain string or raw bytes (ZIP container).
     */
    fun extractJsonFromTbContent(tbContent: String): String {
        val trimmed = tbContent.trim()
        if (trimmed.startsWith("{")) return trimmed

        // Try base64 decoding if it was passed as base64-encoded zip
        try {
            val bytes = Base64.decode(trimmed, Base64.DEFAULT)
            if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "data.json") {
                            return zis.bufferedReader(Charsets.UTF_8).readText()
                        }
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            // Not base64
        }
        return trimmed
    }

    /**
     * Imports from dedicated .tb file content with conflict resolution (OVERWRITE or CREATE_COPY).
     */
    suspend fun importTbFileContent(
        context: Context,
        repository: TheBiGRepository,
        tbContent: String,
        conflictResolution: ConflictResolution = ConflictResolution.OVERWRITE
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonStr = extractJsonFromTbContent(tbContent)
            val root = JSONObject(jsonStr)

            // If CREATE_COPY mode, append " (Copia)" to conflicting tournaments & teams
            if (conflictResolution == ConflictResolution.CREATE_COPY) {
                val existingTeams = repository.getAllTeamsOnce().map { tm -> tm.name.trim().lowercase() }.toSet()
                val existingTourneys = repository.getAllTournamentsOnce().map { tr -> tr.name.trim().lowercase() }.toSet()

                if (root.has("tournament")) {
                    val tObj = root.getJSONObject("tournament")
                    val orig = tObj.getString("name")
                    if (existingTourneys.contains(orig.trim().lowercase())) {
                        tObj.put("name", "$orig (Copia)")
                    }
                }
                if (root.has("tournaments")) {
                    val tArr = root.getJSONArray("tournaments")
                    for (i in 0 until tArr.length()) {
                        val tObj = tArr.getJSONObject(i)
                        val orig = tObj.getString("name")
                        if (existingTourneys.contains(orig.trim().lowercase())) {
                            tObj.put("name", "$orig (Copia)")
                        }
                    }
                }
                if (root.has("team")) {
                    val tObj = root.getJSONObject("team")
                    val orig = tObj.getString("name")
                    if (existingTeams.contains(orig.trim().lowercase())) {
                        tObj.put("name", "$orig (Copia)")
                    }
                }
                if (root.has("teams")) {
                    val tArr = root.getJSONArray("teams")
                    for (i in 0 until tArr.length()) {
                        val tObj = tArr.getJSONObject(i)
                        val orig = tObj.getString("name")
                        if (existingTeams.contains(orig.trim().lowercase())) {
                            tObj.put("name", "$orig (Copia)")
                        }
                    }
                }
            }

            // Extract any custom shield images if present
            val teamArr = root.optJSONArray("teams")
            if (teamArr != null) {
                val crestDir = File(context.filesDir, "team_shields").apply { if (!exists()) mkdirs() }
                for (i in 0 until teamArr.length()) {
                    val tmObj = teamArr.getJSONObject(i)
                    if (tmObj.has("customShieldBase64")) {
                        val base64Str = tmObj.getString("customShieldBase64")
                        val bytes = Base64.decode(base64Str, Base64.DEFAULT)
                        val crestFile = File(crestDir, "imported_shield_${System.currentTimeMillis()}_$i.png")
                        FileOutputStream(crestFile).use { it.write(bytes) }
                        tmObj.put("customShieldUri", Uri.fromFile(crestFile).toString())
                    }
                }
            }

            // Handle SINGLE_TEAM import directly
            if (root.optString("type") == "SINGLE_TEAM" && root.has("team")) {
                val teamObj = root.getJSONObject("team")
                val teamId = repository.saveTeam(
                    Team(
                        name = teamObj.getString("name"),
                        shortName = teamObj.getString("shortName"),
                        shieldVectorIndex = teamObj.optInt("shieldVectorIndex", 0),
                        shieldPrimaryColorHex = teamObj.optString("shieldPrimaryColorHex", "#F59E0B"),
                        primaryColorHex = teamObj.optString("primaryColorHex", "#0284C7"),
                        secondaryColorHex = teamObj.optString("secondaryColorHex", "#0F172A"),
                        borderColorHex = teamObj.optString("borderColorHex", "#FFFFFF"),
                        jerseyPattern = teamObj.optString("jerseyPattern", "STRIPES_VERTICAL"),
                        goalkeeperColorHex = teamObj.optString("goalkeeperColorHex", "#10B981")
                    )
                )
                val playersArr = root.optJSONArray("players")
                if (playersArr != null) {
                    for (j in 0 until playersArr.length()) {
                        val pObj = playersArr.getJSONObject(j)
                        repository.savePlayer(
                            Player(
                                teamId = teamId,
                                name = pObj.getString("name"),
                                dorsal = pObj.getInt("dorsal"),
                                isGoalkeeper = pObj.optBoolean("isGoalkeeper", false),
                                isCaptain = pObj.optBoolean("isCaptain", false)
                            )
                        )
                    }
                }
                return@withContext true
            }

            repository.importDatabaseFromJson(root.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads text/JSON content directly from a file Uri (supporting raw JSON, Base64, and .tb ZIP archives).
     */
    fun readTbContentFromUri(context: Context, uri: Uri): String {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return ""
            if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "data.json") {
                            return zis.bufferedReader(Charsets.UTF_8).readText()
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            val rawStr = String(bytes, Charsets.UTF_8).trim()
            if (rawStr.startsWith("{")) return rawStr
            try {
                val decoded = Base64.decode(rawStr, Base64.DEFAULT)
                if (decoded.size >= 4 && decoded[0] == 0x50.toByte() && decoded[1] == 0x4B.toByte()) {
                    ZipInputStream(ByteArrayInputStream(decoded)).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == "data.json") {
                                return zis.bufferedReader(Charsets.UTF_8).readText()
                            }
                            entry = zis.nextEntry
                        }
                    }
                }
                val decStr = String(decoded, Charsets.UTF_8).trim()
                if (decStr.startsWith("{")) return decStr
            } catch (e: Exception) {
                // Not base64
            }
            return rawStr
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * Shares the exported .tb file via standard Android share sheet.
     */
    fun shareTbFile(context: Context, fileUri: Uri, tournamentName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Copia de Seguridad TheBiG (.tb) - $tournamentName")
            putExtra(Intent.EXTRA_TEXT, "Archivo exclusivo de respaldo .tb de TheBiG para $tournamentName.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Archivo .tb en TheBiG"))
    }

    /**
     * Creates a compact spectator JSON snapshot for QR sharing.
     */
    fun createSpectatorPayload(
        tournament: Tournament,
        teams: List<Team>,
        matches: List<Match>
    ): String {
        val obj = JSONObject()
        obj.put("app", "TheBiG")
        obj.put("role", "SPECTATOR")
        obj.put("tName", tournament.name)
        obj.put("tEd", tournament.edition)
        obj.put("tStatus", tournament.status)

        val teamsArr = JSONArray()
        teams.forEach { t ->
            val tObj = JSONObject()
            tObj.put("id", t.id)
            tObj.put("name", t.name)
            tObj.put("code", t.shortName)
            teamsArr.put(tObj)
        }
        obj.put("teams", teamsArr)

        val matchesArr = JSONArray()
        matches.take(30).forEach { m ->
            val mObj = JSONObject()
            mObj.put("h", m.homeTeamId)
            mObj.put("a", m.awayTeamId)
            mObj.put("hs", m.homeScore)
            mObj.put("as", m.awayScore)
            mObj.put("st", m.status)
            mObj.put("rd", m.roundName)
            matchesArr.put(mObj)
        }
        obj.put("matches", matchesArr)

        return obj.toString()
    }
}
