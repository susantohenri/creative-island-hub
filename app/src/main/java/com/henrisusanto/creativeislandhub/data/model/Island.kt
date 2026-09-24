package com.henrisusanto.creativeislandhub.data.model

import kotlinx.serialization.Serializable

@Serializable
data class IslandDataResponse(
    val data: List<Island> = emptyList()
)

@Serializable
data class Island(
    val code: String,
    val title: String,
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val creatorCode: String? = null,
    val createdIn: String? = null
) {
    val thumbnailUrl: String
        get() = "https://raw.githubusercontent.com/susantohenri/creative-island-hub/refs/heads/main/content/thumbnails/$code.webp"
}
