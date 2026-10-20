package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.entity.Tournament
import com.example.domain.rules.ScorerStat
import com.example.domain.rules.TeamStanding

object WhatsAppShareHelper {

    const val WHATSAPP_CONTACT_URL = "https://wa.me/5363270465"

    fun openSupportWhatsApp(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_CONTACT_URL)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Generates clean formatted text without emojis for WhatsApp groups.
     */
    fun buildStandingsText(
        tournament: Tournament,
        standings: List<TeamStanding>,
        scorers: List<ScorerStat>
    ): String {
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append(" TORNEO: ${tournament.name.uppercase()}\n")
        sb.append(" EDICION: ${tournament.edition}\n")
        sb.append(" TABLA DE POSICIONES\n")
        sb.append("========================================\n")
        sb.append("POS | EQUIPO          | PJ | DG | PTS\n")
        sb.append("----+-----------------+----+----+----\n")

        for (s in standings) {
            val name = if (s.team.name.length > 15) s.team.name.substring(0, 15) else s.team.name.padEnd(15)
            val pos = "%2d".format(s.position)
            val pj = "%2d".format(s.pj)
            val dg = if (s.dg >= 0) "+%2d".format(s.dg) else "%3d".format(s.dg)
            val pts = "%3d".format(s.pts)
            sb.append("$pos  | $name | $pj | $dg | $pts\n")
        }

        if (scorers.isNotEmpty()) {
            sb.append("\n========================================\n")
            sb.append(" GOLEADORES (TOP 5)\n")
            sb.append("========================================\n")
            scorers.take(5).forEachIndexed { i, sc ->
                val teamName = sc.team?.name ?: ""
                sb.append("${i + 1}. ${sc.player.name} ($teamName) - ${sc.goals} goles\n")
            }
        }

        sb.append("\nGenerado por TheBiG - Desarrollado por Daniel (el Grande)\n")
        return sb.toString()
    }

    fun shareTextToWhatsApp(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            // Attempt WhatsApp direct if installed
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general system share sheet
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "Compartir en WhatsApp"
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        }
    }
}
