package com.sepi.market.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Kumpulan fungsi utilitas pemformatan nilai mata uang, waktu relatif, hitung mundur, dan nomor VA.
 */
object Formatters {

    private val rupiahSymbols = DecimalFormatSymbols().apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val rupiahFormatter = DecimalFormat("#,###", rupiahSymbols)

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))

    /**
     * Format nominal uang ke mata uang Rupiah tanpa spasi: Rp199.000.
     */
    fun formatRupiah(amount: Long): String {
        return "Rp${rupiahFormatter.format(amount)}"
    }

    /**
     * Format nomor Virtual Account: "1234567890" -> "123 456 7890".
     */
    fun formatVa(va: String): String {
        val clean = va.replace(" ", "")
        return if (clean.length == 10) {
            "${clean.substring(0, 3)} ${clean.substring(3, 6)} ${clean.substring(6)}"
        } else {
            clean.chunked(4).joinToString(" ")
        }
    }

    /**
     * Hitung persentase diskon dari harga asli: pembulatan ke bilangan bulat.
     */
    fun discountPercent(price: Long, originalPrice: Long?): Int {
        if (originalPrice == null || originalPrice <= price || originalPrice <= 0) return 0
        val discount = ((originalPrice - price).toDouble() / originalPrice.toDouble()) * 100.0
        return kotlin.math.round(discount).toInt()
    }

    /**
     * Mengubah string ISO-8601 menjadi teks waktu relatif berbahasa Indonesia:
     * "baru saja", "5 menit lalu", "3 hari lalu", atau "d MMM yyyy".
     */
    fun relativeTime(iso: String): String {
        return try {
            val dateTime = OffsetDateTime.parse(iso)
            val now = OffsetDateTime.now(dateTime.offset)
            val duration = Duration.between(dateTime, now)

            val seconds = duration.seconds
            when {
                seconds < 60 -> "baru saja"
                seconds < 3600 -> "${seconds / 60} menit lalu"
                seconds < 86400 -> "${seconds / 3600} jam lalu"
                seconds < 86400 * 7 -> "${seconds / 86400} hari lalu"
                else -> dateTime.format(dateFormatter)
            }
        } catch (_: Exception) {
            iso
        }
    }

    /**
     * Menghitung sisa waktu menuju [deadlineIso] dan memformatnya menjadi format "HH:MM:SS".
     * Mengembalikan "00:00:00" jika batas waktu sudah lewat.
     */
    fun countdown(deadlineIso: String): String {
        return try {
            val deadline = OffsetDateTime.parse(deadlineIso)
            val now = OffsetDateTime.now(deadline.offset)
            val secondsRemaining = Duration.between(now, deadline).seconds
            if (secondsRemaining <= 0) {
                "00:00:00"
            } else {
                formatSecondsToHms(secondsRemaining)
            }
        } catch (_: Exception) {
            "00:00:00"
        }
    }

    /**
     * Format detik menjadi "HH:MM:SS".
     */
    fun formatSecondsToHms(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Menghasilkan waktu akhir flash sale berikutnya (kelipatan 3 jam berikutnya pada zona waktu perangkat).
     */
    fun nextFlashSaleEnd(): OffsetDateTime {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val currentHour = now.hour
        val nextHour = ((currentHour / 3) + 1) * 3
        val target = if (nextHour >= 24) {
            now.plusDays(1).withHour(nextHour % 24).withMinute(0).withSecond(0).withNano(0)
        } else {
            now.withHour(nextHour).withMinute(0).withSecond(0).withNano(0)
        }
        return target.toOffsetDateTime()
    }
}
