package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.content.Context
import com.example.data.model.AspectRatio
import com.example.data.model.AudioTrackItem
import com.example.data.model.AudioTrackType
import com.example.data.model.Project
import com.example.data.model.SubtitleCue
import com.example.data.model.SubtitlePosition
import com.example.data.model.SubtitleStyle
import com.example.data.model.TextOverlay
import com.example.data.model.VideoClip
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val lastModifiedAt: Long,
    val aspectRatioName: String,
    val originalLanguage: String,
    val targetLanguage: String,
    val originalVideoVolume: Float,
    val isOriginalVideoMuted: Boolean,
    val clipsJson: String,
    val subtitlesJson: String,
    val audioTracksJson: String,
    val textOverlaysJson: String,
    val subtitleStyleJson: String
)

class ProjectTypeConverters {
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val clipListType = Types.newParameterizedType(List::class.java, VideoClip::class.java)
    private val subtitleListType = Types.newParameterizedType(List::class.java, SubtitleCue::class.java)
    private val audioListType = Types.newParameterizedType(List::class.java, AudioTrackItem::class.java)
    private val textOverlayListType = Types.newParameterizedType(List::class.java, TextOverlay::class.java)

    fun clipsToJson(clips: List<VideoClip>): String = moshi.adapter<List<VideoClip>>(clipListType).toJson(clips)
    fun jsonToClips(json: String): List<VideoClip> = try {
        moshi.adapter<List<VideoClip>>(clipListType).fromJson(json) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    fun subtitlesToJson(subs: List<SubtitleCue>): String = moshi.adapter<List<SubtitleCue>>(subtitleListType).toJson(subs)
    fun jsonToSubtitles(json: String): List<SubtitleCue> = try {
        moshi.adapter<List<SubtitleCue>>(subtitleListType).fromJson(json) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    fun audioToJson(audio: List<AudioTrackItem>): String = moshi.adapter<List<AudioTrackItem>>(audioListType).toJson(audio)
    fun jsonToAudio(json: String): List<AudioTrackItem> = try {
        moshi.adapter<List<AudioTrackItem>>(audioListType).fromJson(json) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    fun textOverlaysToJson(texts: List<TextOverlay>): String = moshi.adapter<List<TextOverlay>>(textOverlayListType).toJson(texts)
    fun jsonToTextOverlays(json: String): List<TextOverlay> = try {
        moshi.adapter<List<TextOverlay>>(textOverlayListType).fromJson(json) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    fun styleToJson(style: SubtitleStyle): String = moshi.adapter(SubtitleStyle::class.java).toJson(style)
    fun jsonToStyle(json: String): SubtitleStyle = try {
        moshi.adapter(SubtitleStyle::class.java).fromJson(json) ?: SubtitleStyle()
    } catch (_: Exception) { SubtitleStyle() }
}

fun Project.toEntity(converters: ProjectTypeConverters = ProjectTypeConverters()): ProjectEntity {
    return ProjectEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        aspectRatioName = aspectRatio.name,
        originalLanguage = originalLanguage,
        targetLanguage = targetLanguage,
        originalVideoVolume = originalVideoVolume,
        isOriginalVideoMuted = isOriginalVideoMuted,
        clipsJson = converters.clipsToJson(clips),
        subtitlesJson = converters.subtitlesToJson(subtitles),
        audioTracksJson = converters.audioToJson(audioTracks),
        textOverlaysJson = converters.textOverlaysToJson(textOverlays),
        subtitleStyleJson = converters.styleToJson(subtitleStyle)
    )
}

fun ProjectEntity.toDomain(converters: ProjectTypeConverters = ProjectTypeConverters()): Project {
    return Project(
        id = id,
        name = name,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        aspectRatio = try { AspectRatio.valueOf(aspectRatioName) } catch (_: Exception) { AspectRatio.RATIO_9_16 },
        originalLanguage = originalLanguage,
        targetLanguage = targetLanguage,
        originalVideoVolume = originalVideoVolume,
        isOriginalVideoMuted = isOriginalVideoMuted,
        clips = converters.jsonToClips(clipsJson),
        subtitles = converters.jsonToSubtitles(subtitlesJson),
        audioTracks = converters.jsonToAudio(audioTracksJson),
        textOverlays = converters.jsonToTextOverlays(textOverlaysJson),
        subtitleStyle = converters.jsonToStyle(subtitleStyleJson)
    )
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModifiedAt DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bhasha_studio.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
