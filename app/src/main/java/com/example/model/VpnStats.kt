package com.example.model

data class VpnStats(
    val txBytes: Long = 0,
    val rxBytes: Long = 0,
    val txPackets: Long = 0,
    val rxPackets: Long = 0,
    val durationSeconds: Long = 0
) {
    fun formatTx(): String = formatBytes(txBytes)
    fun formatRx(): String = formatBytes(rxBytes)

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %cB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }

    fun formatDuration(): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
