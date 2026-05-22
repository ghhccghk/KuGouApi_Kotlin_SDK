package com.ghhccghk.multiplatform.kugouapi.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SongQuality(val value: String) {
    @SerialName("piano") PIANO("piano"),
    @SerialName("acappella") ACAPPELLA("acappella"),
    @SerialName("subwoofer") SUBWOOFER("subwoofer"),
    @SerialName("ancient") ANCIENT("ancient"),
    @SerialName("surnay") SURNAY("surnay"),
    @SerialName("dj") DJ("dj"),
    @SerialName("128") STANDARD("128"),
    @SerialName("320") HIGH("320"),
    @SerialName("flac") LOSSLESS("flac"),
    @SerialName("high") HI_RES("high"),
    @SerialName("viper_atmos") VIPER_ATMOS("viper_atmos"),
    @SerialName("viper_clear") VIPER_CLEAR("viper_clear"),
    @SerialName("viper_tape") VIPER_TAPE("viper_tape"),
    @SerialName("super") SUPER("super")
}

@Serializable
enum class SearchType(val value: String) {
    @SerialName("song") SONG("song"),
    @SerialName("special") SPECIAL("special"),
    @SerialName("lyric") LYRIC("lyric"),
    @SerialName("album") ALBUM("album"),
    @SerialName("author") AUTHOR("author"),
    @SerialName("mv") MV("mv")
}

@Serializable
enum class FmMode(val value: String) {
    @SerialName("normal") NORMAL("normal"),
    @SerialName("small") SMALL("small"),
    @SerialName("peak") PEAK("peak")
}

@Serializable
enum class FmAction(val value: String) {
    @SerialName("play") PLAY("play"),
    @SerialName("garbage") GARBAGE("garbage")
}

@Serializable
enum class AlbumType(val value: Int) {
    @SerialName("1") TYPE_1(1),
    @SerialName("2") TYPE_2(2),
    @SerialName("3") TYPE_3(3),
    @SerialName("4") TYPE_4(4)
}

@Serializable
enum class ArtistSexType(val value: Int) {
    @SerialName("0") ALL(0),
    @SerialName("1") MALE(1),
    @SerialName("2") FEMALE(2),
    @SerialName("3") GROUP(3)
}

@Serializable
enum class ArtistRegionType(val value: Int) {
    @SerialName("0") ALL(0),
    @SerialName("1") CHINA(1),
    @SerialName("2") EUROPE(2),
    @SerialName("3") JAPAN(3),
    @SerialName("4") KOREA(4),
    @SerialName("5") OTHER(5),
    @SerialName("6") SOUTHEAST_ASIA(6)
}

@Serializable
enum class ArtistVideoTag(val value: String) {
    @SerialName("official") OFFICIAL("official"),
    @SerialName("live") LIVE("live"),
    @SerialName("fan") FAN("fan"),
    @SerialName("artist") ARTIST("artist"),
    @SerialName("all") ALL("all")
}

@Serializable
enum class ArtistSort(val value: String) {
    @SerialName("hot") HOT("hot"),
    @SerialName("new") NEW("new")
}

@Serializable
enum class SceneSort(val value: String) {
    @SerialName("rec") REC("rec"),
    @SerialName("hot") HOT("hot"),
    @SerialName("new") NEW("new")
}

@Serializable
enum class AudioRelatedSort(val value: String) {
    @SerialName("all") ALL("all"),
    @SerialName("hot") HOT("hot"),
    @SerialName("new") NEW("new")
}

@Serializable
enum class CommentSort(val value: Int) {
    @SerialName("1") TIME(1),
    @SerialName("2") HOT(2)
}

@Serializable
enum class EncryptType {
    ANDROID,
    WEB,
    REGISTER
}

@Serializable
enum class IpDataType(val value: String) {
    @SerialName("audios") AUDIOS("audios"),
    @SerialName("albums") ALBUMS("albums"),
    @SerialName("videos") VIDEOS("videos"),
    @SerialName("author_list") AUTHOR_LIST("author_list")
}

@Serializable
enum class FollowNewSongsSort(val value: Int) {
    @SerialName("1") TIME(1),
    @SerialName("2") HOT(2)
}
