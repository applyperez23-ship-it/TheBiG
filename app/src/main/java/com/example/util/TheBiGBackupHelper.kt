package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object TheBiGBackupHelper {

    /**
     * Checks if the device has an active internet or local network connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
     * Exports full tournament state to dedicated .tb file format: Torneo_<Nombre>_TheBiG.tb
     * Encodes custom shields to Base64 to preserve custom crests across devices.
     */
    suspend fun exportTournamentTbFile(
        context: Context,
        repository: TheBiGRepository,
        tournamentId: Long
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("format", "THEBIG_TB")
            root.put("version", 5)
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
                put("prizeChampionAmount", tournament.prizeChampionAmount)
                put("prizeRunnerUpAmount", tournament.prizeRunnerUpAmount)
                put("prizeTopScorerAmount", tournament.prizeTopScorerAmount)
                put("prizeBestGkAmount", tournament.prizeBestGkAmount)
            }
            root.put("tournament", tObj)

            val teams = repository.getTeamsForTournamentOnce(tournamentId)
            val teamArr = JSONArray()
            for (team in teams) {
                val obj = JSONObject().apply {
                    put("id", team.id)
                    put("name", team.name)
                    put("shortName", team.shortName)
                    put("shieldVectorIndex", team.shieldVectorIndex)
                    put("primaryColorHex", team.primaryColorHex)
                    put("secondaryColorHex", team.secondaryColorHex)
                    put("borderColorHex", team.borderColorHex)
                    put("jerseyPattern", team.jerseyPattern)
                    put("goalkeeperColorHex", team.goalkeeperColorHex)

                    // Base64 encode custom crest file if present
                    if (team.customShieldUri != null) {
                        try {
                            val uri = Uri.parse(team.customShieldUri)
                            val path = uri.path
                            if (path != null) {
                                val file = File(path)
                                if (file.exists()) {
                                    val bytes = FileInputStream(file).use { it.readBytes() }
                                    put("customShieldBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))
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
            val file = File(exportDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }

            FileProvider.getUriForFile(
                context,
                "com.aistudio.thebig.ftbol.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports full database to dedicated .tb file: TheBiG_Full_Backup.tb
     */
    suspend fun exportFullDatabaseTbFile(
        context: Context,
        repository: TheBiGRepository
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val jsonContent = repository.exportDatabaseToJson()
            val root = JSONObject(jsonContent)
            root.put("format", "THEBIG_TB")

            val fileName = "TheBiG_Full_Backup.tb"
            val exportDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
            val file = File(exportDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }

            FileProvider.getUriForFile(
                context,
                "com.aistudio.thebig.ftbol.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Imports from dedicated .tb file or JSON content.
     */
    suspend fun importTbFileContent(
        context: Context,
        repository: TheBiGRepository,
        tbContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(tbContent)

            // Process teams and decode base64 custom shields if present
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

            repository.importDatabaseFromJson(root.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
            putExtra(Intent.EXTRA_TEXT, "Archivo exclusivo de respaldo .tb de TheBiG para el torneo $tournamentName.")
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
