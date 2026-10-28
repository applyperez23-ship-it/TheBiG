package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.data.repository.TheBiGRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object TheBiGBackupHelper {

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
     * Exports full tournament state to JSON string and saves file Torneo_<Nombre>_TheBiG.json.
     */
    suspend fun exportTournamentJson(
        context: Context,
        repository: TheBiGRepository,
        tournamentId: Long
    ): Uri? {
        val jsonString = repository.exportTournamentToJson(tournamentId)
        val tournament = repository.getTournamentOnce(tournamentId) ?: return null

        val sanitizedName = tournament.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val fileName = "Torneo_${sanitizedName}_TheBiG.json"

        val exportDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
        val file = File(exportDir, fileName)
        FileOutputStream(file).use { out ->
            out.write(jsonString.toByteArray(Charsets.UTF_8))
        }

        return FileProvider.getUriForFile(
            context,
            "com.aistudio.thebig.ftbol.fileprovider",
            file
        )
    }

    /**
     * Shares the exported JSON file via standard Android share sheet.
     */
    fun shareBackupFile(context: Context, fileUri: Uri, tournamentName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Copia de Seguridad TheBiG - $tournamentName")
            putExtra(Intent.EXTRA_TEXT, "Copia de respaldo del torneo $tournamentName en TheBiG.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Torneo en TheBiG"))
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
