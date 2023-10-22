package com.example.composestates

import java.util.*

enum class Icon {
    CAMERA,
    SHARE,
    MORE_OPTIONS,
    AUDIO,
    LIKE,
    COMMENT
}

data class Reel(
    val reelUrl: String,
    val isFollowed: Boolean,
    val reelInfo: ReelInfo
)

data class ReelInfo(
    val name: String? = null,
    var path: String? = null,
    var url: String? = null,
    val userId: Int? = null,
    val userProfileUrl: String? = null,
    var sizeInMB: String? = null,
    val categoryId: Int? = null,
    val userName: String? = null,
    var likeCount: Long = 0,
    var commentCount: Long = 0,
    var downloadCount: Long = 0,
    val createdAt: String? = null,
    var thumbnail: String? = null,
    var firstFrame: String? = null,
    val isAlreadyLiked: Boolean = false,
    val shareLink: String? = null,
    var id: Int = 0,
    var viewTypeId: Int? = null,
    val instaUrl: String?= null,
    var fileType: String?= null
)