package com.example.ui.editor

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AspectRatio
import com.example.data.model.AudioTrackItem
import com.example.data.model.ExportConfig
import com.example.data.model.LanguageCatalog
import com.example.data.model.Project
import com.example.data.model.SubtitleCue
import com.example.data.model.SubtitleStyle
import com.example.data.model.SupportedLanguage
import com.example.data.model.TextOverlay
import com.example.data.model.VideoClip
import com.example.data.repository.ProjectRepository
import com.example.domain.ai.DubbingVoice
import com.example.domain.ai.MultilingualTranslationService
import com.example.domain.ai.SpeechTranscriptionService
import com.example.domain.ai.TextToSpeechDubbingService
import com.example.domain.audio.SoundEffectsLibrary
import com.example.domain.audio.SoundPreset
import com.example.domain.audio.VoiceOverRecorder
import com.example.domain.video.ExportResult
import com.example.domain.video.SrtParser
import com.example.domain.video.VideoExporter
import com.example.domain.video.VideoMetadataHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class ActivePanel {
    data object NONE : ActivePanel()
    data object DUBBING : ActivePanel()
    data object SUBTITLES : ActivePanel()
    data object AUDIO_MIXER : ActivePanel()
    data object TEXT_OVERLAY : ActivePanel()
    data object EXPORT : ActivePanel()
    data object PROJECTS : ActivePanel()
}

class VideoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    private val translationService = MultilingualTranslationService()
    val ttsService = TextToSpeechDubbingService(application)
    private val transcriptionService = SpeechTranscriptionService(application)
    val voiceRecorder = VoiceOverRecorder(application)
    val sfxLibrary = SoundEffectsLibrary(application)
    private val videoHelper = VideoMetadataHelper(application)
    private val videoExporter = VideoExporter(application)

    // Current Project
    private val _project = MutableStateFlow(Project(name = "Bhasha Studio Project"))
    val project: StateFlow<Project> = _project.asStateFlow()

    // Saved projects list
    val savedProjects: StateFlow<List<Project>> = repository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Playback state
    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Active bottom panel
    private val _activePanel = MutableStateFlow<ActivePanel>(ActivePanel.NONE)
    val activePanel: StateFlow<ActivePanel> = _activePanel.asStateFlow()

    // Export state
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow("")
    val exportStatusMessage: StateFlow<String> = _exportStatusMessage.asStateFlow()

    private val _lastExportResult = MutableStateFlow<ExportResult?>(null)
    val lastExportResult: StateFlow<ExportResult?> = _lastExportResult.asStateFlow()

    // AI Operation Busy State
    private val _isAiBusy = MutableStateFlow(false)
    val isAiBusy: StateFlow<Boolean> = _isAiBusy.asStateFlow()

    private val _aiStatusMessage = MutableStateFlow("")
    val aiStatusMessage: StateFlow<String> = _aiStatusMessage.asStateFlow()

    // Selected Voice for Dubbing
    private val _selectedVoice = MutableStateFlow<DubbingVoice?>(null)
    val selectedVoice: StateFlow<DubbingVoice?> = _selectedVoice.asStateFlow()

    private val _ttsPitch = MutableStateFlow(1.0f)
    val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

    private val _ttsRate = MutableStateFlow(1.0f)
    val ttsRate: StateFlow<Float> = _ttsRate.asStateFlow()

    // Selected subtitle or clip for granular editing
    private val _selectedSubtitleId = MutableStateFlow<String?>(null)
    val selectedSubtitleId: StateFlow<String?> = _selectedSubtitleId.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private var playbackTickerJob: Job? = null
    var mediaPlayer: MediaPlayer? = null
    private var bgmPlayer: MediaPlayer? = null
    private var dubbingPlayer: MediaPlayer? = null

    init {
        // Refresh voices once TTS initializes
        viewModelScope.launch {
            ttsService.isInitialized.collect { ready ->
                if (ready) {
                    val target = LanguageCatalog.fromCode(_project.value.targetLanguage)
                    ttsService.refreshAvailableVoices(target)
                }
            }
        }
        viewModelScope.launch {
            ttsService.availableVoices.collect { voices ->
                if (voices.isNotEmpty() && _selectedVoice.value == null) {
                    _selectedVoice.value = voices.firstOrNull()
                }
            }
        }

        // Auto load initial demo sample so user can edit right away
        loadSampleProject()
    }

    fun openPanel(panel: ActivePanel) {
        _activePanel.value = panel
    }

    fun closePanel() {
        _activePanel.value = ActivePanel.NONE
    }

    fun selectSubtitle(id: String?) {
        _selectedSubtitleId.value = id
    }

    fun selectClip(id: String?) {
        _selectedClipId.value = id
    }

    fun setPitch(pitch: Float) {
        _ttsPitch.value = pitch.coerceIn(0.5f, 2.0f)
    }

    fun setSpeechRate(rate: Float) {
        _ttsRate.value = rate.coerceIn(0.5f, 2.0f)
    }

    fun selectVoice(voice: DubbingVoice) {
        _selectedVoice.value = voice
    }

    // --- Video Clip Management ---

    fun loadSampleProject() {
        viewModelScope.launch {
            _isAiBusy.value = true
            _aiStatusMessage.value = "Initializing Bhasha Studio sample project..."
            val sampleClip = videoHelper.generateSampleStudioVideo("Bhasha Studio Intro")
            val initialCues = listOf(
                SubtitleCue(
                    startMs = 500L,
                    endMs = 3800L,
                    text = "Welcome to Bhasha Studio, the pro video editor!",
                    originalText = "Welcome to Bhasha Studio, the pro video editor!",
                    languageCode = "en"
                ),
                SubtitleCue(
                    startMs = 4200L,
                    endMs = 7500L,
                    text = "Translate subtitles and dub your voice in Hindi automatically.",
                    originalText = "Translate subtitles and dub your voice in Hindi automatically.",
                    languageCode = "en"
                ),
                SubtitleCue(
                    startMs = 8000L,
                    endMs = 11500L,
                    text = "Export cinematic reels in 1080p with burned-in subtitles.",
                    originalText = "Export cinematic reels in 1080p with burned-in subtitles.",
                    languageCode = "en"
                )
            )

            val newProj = Project(
                name = "Sample Studio Project",
                clips = listOf(sampleClip),
                subtitles = initialCues,
                originalLanguage = "en",
                targetLanguage = "hi",
                aspectRatio = AspectRatio.RATIO_9_16
            )
            _project.value = newProj
            _selectedClipId.value = sampleClip.id
            saveCurrentProject()
            _isAiBusy.value = false
        }
    }

    fun importVideo(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            _isAiBusy.value = true
            _aiStatusMessage.value = "Importing video to studio cache..."
            val clip = videoHelper.importVideoFromUri(uri, displayName)
            if (clip != null) {
                val updatedClips = _project.value.clips + clip
                _project.value = _project.value.copy(clips = updatedClips)
                _selectedClipId.value = clip.id
                saveCurrentProject()
            }
            _isAiBusy.value = false
        }
    }

    fun trimSelectedClip(newTrimStartMs: Long, newTrimEndMs: Long) {
        val clipId = _selectedClipId.value ?: return
        val currentClips = _project.value.clips.toMutableList()
        val index = currentClips.indexOfFirst { it.id == clipId }
        if (index >= 0) {
            val clip = currentClips[index]
            val safeStart = newTrimStartMs.coerceIn(0L, clip.durationMs - 200L)
            val safeEnd = newTrimEndMs.coerceIn(safeStart + 200L, clip.durationMs)
            currentClips[index] = clip.copy(trimStartMs = safeStart, trimEndMs = safeEnd)
            _project.value = _project.value.copy(clips = currentClips)
            saveCurrentProject()
        }
    }

    fun splitClipAtPlayhead() {
        val playhead = _currentTimeMs.value
        val currentClips = _project.value.clips.toMutableList()

        // Find which clip contains the playhead
        var accumulatedMs = 0L
        for (i in currentClips.indices) {
            val clip = currentClips[i]
            val clipDuration = clip.effectiveDurationMs
            if (playhead > accumulatedMs + 200L && playhead < accumulatedMs + clipDuration - 200L) {
                // Split here
                val splitOffsetInClip = ((playhead - accumulatedMs) * clip.speed).toLong()
                val splitPointMs = clip.trimStartMs + splitOffsetInClip

                val firstHalf = clip.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${clip.name} (Part 1)",
                    trimEndMs = splitPointMs
                )
                val secondHalf = clip.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${clip.name} (Part 2)",
                    trimStartMs = splitPointMs
                )

                currentClips.removeAt(i)
                currentClips.add(i, secondHalf)
                currentClips.add(i, firstHalf)
                _project.value = _project.value.copy(clips = currentClips)
                saveCurrentProject()
                break
            }
            accumulatedMs += clipDuration
        }
    }

    fun rotateSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        val currentClips = _project.value.clips.toMutableList()
        val index = currentClips.indexOfFirst { it.id == clipId }
        if (index >= 0) {
            val clip = currentClips[index]
            val nextRotation = (clip.rotationDegrees + 90) % 360
            currentClips[index] = clip.copy(rotationDegrees = nextRotation)
            _project.value = _project.value.copy(clips = currentClips)
            saveCurrentProject()
        }
    }

    fun setAspectRatio(ratio: AspectRatio) {
        _project.value = _project.value.copy(aspectRatio = ratio)
        saveCurrentProject()
    }

    fun deleteSelectedClip() {
        val clipId = _selectedClipId.value ?: return
        val updated = _project.value.clips.filterNot { it.id == clipId }
        _project.value = _project.value.copy(clips = updated)
        _selectedClipId.value = updated.firstOrNull()?.id
        saveCurrentProject()
    }

    // --- Subtitles & AI Translation ---

    fun autoTranscribeSpeech() {
        viewModelScope.launch {
            val clip = _project.value.clips.firstOrNull() ?: return@launch
            _isAiBusy.value = true
            _aiStatusMessage.value = "Transcribing dialogue speech from audio..."
            val cues = transcriptionService.transcribeVideoAudio(
                clip.localFilePath,
                _project.value.totalDurationMs
            ) { progress ->
                _aiStatusMessage.value = "Detecting dialogue cues: ${(progress * 100).toInt()}%"
            }

            _project.value = _project.value.copy(subtitles = cues)
            saveCurrentProject()
            _isAiBusy.value = false
        }
    }

    fun translateSubtitles(targetLangCode: String) {
        viewModelScope.launch {
            _isAiBusy.value = true
            _aiStatusMessage.value = "Translating speech to ${LanguageCatalog.fromCode(targetLangCode).name}..."

            val translatedCues = _project.value.subtitles.map { cue ->
                val translated = translationService.translateText(
                    text = cue.originalText,
                    sourceLangCode = _project.value.originalLanguage,
                    targetLangCode = targetLangCode
                )
                cue.copy(text = translated, languageCode = targetLangCode)
            }

            _project.value = _project.value.copy(
                subtitles = translatedCues,
                targetLanguage = targetLangCode
            )
            // Also refresh TTS voices for new language
            ttsService.refreshAvailableVoices(LanguageCatalog.fromCode(targetLangCode))
            saveCurrentProject()
            _isAiBusy.value = false
        }
    }

    fun updateSubtitleCue(id: String, newText: String, startMs: Long, endMs: Long) {
        val updated = _project.value.subtitles.map { cue ->
            if (cue.id == id) {
                cue.copy(
                    text = newText,
                    startMs = startMs.coerceAtLeast(0L),
                    endMs = endMs.coerceAtLeast(startMs + 100L)
                )
            } else cue
        }
        _project.value = _project.value.copy(subtitles = updated)
        saveCurrentProject()
    }

    fun addManualSubtitleCue() {
        val current = _currentTimeMs.value
        val newCue = transcriptionService.createManualCue(current, 3000L, "नया सबटाइटल")
        val updated = (_project.value.subtitles + newCue).sortedBy { it.startMs }
        _project.value = _project.value.copy(subtitles = updated)
        _selectedSubtitleId.value = newCue.id
        saveCurrentProject()
    }

    fun deleteSubtitleCue(id: String) {
        val updated = _project.value.subtitles.filterNot { it.id == id }
        _project.value = _project.value.copy(subtitles = updated)
        if (_selectedSubtitleId.value == id) _selectedSubtitleId.value = null
        saveCurrentProject()
    }

    fun updateSubtitleStyle(style: SubtitleStyle) {
        _project.value = _project.value.copy(subtitleStyle = style)
        saveCurrentProject()
    }

    fun importSrtFile(content: String) {
        val cues = SrtParser.parseFromSrt(content, _project.value.targetLanguage)
        if (cues.isNotEmpty()) {
            _project.value = _project.value.copy(subtitles = cues)
            saveCurrentProject()
        }
    }

    fun getExportedSrt(): String {
        return SrtParser.exportToSrt(_project.value.subtitles)
    }

    // --- AI Hindi Dubbing ---

    fun previewVoiceForCue(cue: SubtitleCue) {
        val lang = LanguageCatalog.fromCode(_project.value.targetLanguage)
        ttsService.previewSpeech(
            text = cue.text,
            language = lang,
            voiceId = _selectedVoice.value?.id,
            pitch = _ttsPitch.value,
            speechRate = _ttsRate.value
        )
    }

    fun synthesizeFullDubbingTrack() {
        viewModelScope.launch {
            if (_project.value.subtitles.isEmpty()) return@launch
            _isAiBusy.value = true
            _aiStatusMessage.value = "Synthesizing Hindi AI Voice Dubbing..."

            val lang = LanguageCatalog.fromCode(_project.value.targetLanguage)
            val dubbedTracks = ttsService.synthesizeFullDubbing(
                cues = _project.value.subtitles,
                language = lang,
                voiceId = _selectedVoice.value?.id,
                pitch = _ttsPitch.value,
                speechRate = _ttsRate.value
            ) { current, total ->
                _aiStatusMessage.value = "Synthesizing voice: $current of $total segments"
            }

            // Remove previous dubbing tracks and add new synchronized tracks
            val cleanAudio = _project.value.audioTracks.filterNot { it.type == com.example.data.model.AudioTrackType.DUBBING }
            _project.value = _project.value.copy(
                audioTracks = cleanAudio + dubbedTracks,
                // Lower original video audio to 30% so dubbed voice stands out clearly
                originalVideoVolume = 0.3f
            )
            saveCurrentProject()
            _isAiBusy.value = false
        }
    }

    // --- Audio Tools & Mixer ---

    fun addSoundPreset(preset: SoundPreset) {
        viewModelScope.launch {
            val item = sfxLibrary.createTrackItem(preset, _currentTimeMs.value)
            _project.value = _project.value.copy(audioTracks = _project.value.audioTracks + item)
            saveCurrentProject()
        }
    }

    fun setOriginalVideoVolume(volume: Float) {
        _project.value = _project.value.copy(originalVideoVolume = volume.coerceIn(0f, 2f))
        saveCurrentProject()
    }

    fun toggleMuteOriginalVideo() {
        val current = _project.value.isOriginalVideoMuted
        _project.value = _project.value.copy(isOriginalVideoMuted = !current)
        saveCurrentProject()
    }

    fun updateAudioTrackVolume(trackId: String, volume: Float) {
        val updated = _project.value.audioTracks.map { track ->
            if (track.id == trackId) track.copy(volume = volume.coerceIn(0f, 2f)) else track
        }
        _project.value = _project.value.copy(audioTracks = updated)
        saveCurrentProject()
    }

    fun deleteAudioTrack(trackId: String) {
        val updated = _project.value.audioTracks.filterNot { it.id == trackId }
        _project.value = _project.value.copy(audioTracks = updated)
        saveCurrentProject()
    }

    // Voice-over recording
    fun startVoiceOverRecording() {
        voiceRecorder.startRecording(_currentTimeMs.value, viewModelScope)
    }

    fun stopVoiceOverRecording() {
        val trackItem = voiceRecorder.stopRecording(_currentTimeMs.value)
        if (trackItem != null) {
            _project.value = _project.value.copy(audioTracks = _project.value.audioTracks + trackItem)
            saveCurrentProject()
        }
    }

    // --- Text Overlays ---

    fun addTextOverlay(text: String = "Bhasha Studio") {
        val overlay = TextOverlay(
            text = text,
            startMs = _currentTimeMs.value,
            endMs = _currentTimeMs.value + 3500L
        )
        _project.value = _project.value.copy(textOverlays = _project.value.textOverlays + overlay)
        saveCurrentProject()
    }

    fun deleteTextOverlay(id: String) {
        val updated = _project.value.textOverlays.filterNot { it.id == id }
        _project.value = _project.value.copy(textOverlays = updated)
        saveCurrentProject()
    }

    // --- Playback & Seeking ---

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        _isPlaying.value = true
        mediaPlayer?.let { player ->
            try {
                if (!player.isPlaying) {
                    player.seekTo(_currentTimeMs.value.toInt())
                    player.start()
                }
            } catch (_: Exception) {}
        }

        playbackTickerJob?.cancel()
        playbackTickerJob = viewModelScope.launch(Dispatchers.Default) {
            val intervalMs = 33L // approx 30 fps tick
            while (isActive && _isPlaying.value) {
                delay(intervalMs)
                val step = (intervalMs * _playbackSpeed.value).toLong()
                val nextTime = _currentTimeMs.value + step
                val total = _project.value.totalDurationMs

                if (nextTime >= total) {
                    _currentTimeMs.value = 0L
                    withContext(Dispatchers.Main) {
                        pausePlayback()
                    }
                    break
                } else {
                    _currentTimeMs.value = nextTime
                }
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playbackTickerJob?.cancel()
        playbackTickerJob = null
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    fun seekTo(timeMs: Long) {
        val clamped = timeMs.coerceIn(0L, _project.value.totalDurationMs)
        _currentTimeMs.value = clamped
        try {
            mediaPlayer?.seekTo(clamped.toInt())
        } catch (_: Exception) {}
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(speed)
                    ?: android.media.PlaybackParams().setSpeed(speed)
            }
        } catch (_: Exception) {}
    }

    // --- Project Persistence ---

    fun saveCurrentProject() {
        viewModelScope.launch {
            repository.saveProject(_project.value)
        }
    }

    fun loadProject(projectToLoad: Project) {
        pausePlayback()
        _project.value = projectToLoad
        _currentTimeMs.value = 0L
        _selectedClipId.value = projectToLoad.clips.firstOrNull()?.id
        _selectedSubtitleId.value = null
        closePanel()
    }

    fun createNewProject(name: String = "Untitled Project") {
        pausePlayback()
        val newProject = Project(name = name)
        _project.value = newProject
        _currentTimeMs.value = 0L
        _selectedClipId.value = null
        _selectedSubtitleId.value = null
        saveCurrentProject()
        closePanel()
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    // --- MP4 Video Export ---

    fun startExport(config: ExportConfig) {
        viewModelScope.launch {
            pausePlayback()
            _isExporting.value = true
            _exportProgress.value = 0.05f
            _exportStatusMessage.value = "Starting MP4 export..."
            _lastExportResult.value = null

            val result = videoExporter.exportProject(_project.value, config) { progress, message ->
                _exportProgress.value = progress
                _exportStatusMessage.value = message
            }

            _lastExportResult.value = result
            _isExporting.value = false
        }
    }

    fun dismissExportResult() {
        _lastExportResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        pausePlayback()
        ttsService.release()
        try {
            mediaPlayer?.release()
            bgmPlayer?.release()
            dubbingPlayer?.release()
        } catch (_: Exception) {}
    }
}
