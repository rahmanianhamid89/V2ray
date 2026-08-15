package com.example.model

enum class ConfigSortOption(val displayName: String, val shortLabel: String) {
    PING("Latency / Ping", "Ping"),
    TRAFFIC_DESC("Traffic: High to Low", "Traffic ↓"),
    TRAFFIC_ASC("Traffic: Low to High", "Traffic ↑"),
    NAME("Name (A-Z)", "Name")
}
