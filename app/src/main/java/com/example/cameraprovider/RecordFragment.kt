package com.example.cameraprovider

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.cameraprovider.databinding.FragmentRecordBinding
import com.example.cameraprovider.viewmodel.PostViewModel
import rm.com.audiowave.AudioWaveView
import rm.com.audiowave.OnProgressListener
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID

class RecordFragment : Fragment() {

    lateinit var binding: FragmentRecordBinding
    var isRecording: Boolean = false
    var isPlaying: Boolean = false
    private lateinit var fileName: String
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var postViewModel: PostViewModel
    private lateinit var mediaPlayer: MediaPlayer
    private val RECORDING_MEDIA_RECORDER_MAX_DURATION = 60000
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var totalTimer: String
    private val handler = Handler(Looper.getMainLooper())

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value) {
                    permissionGranted = false
                }
            }
            if (!permissionGranted) {
                Toast.makeText(
                    context,
                    "Yêu cầu cấp quyền thu âm",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                isRecording = false
                isPlaying = false
                // Proceed with recording setup
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_record, container, false)
        postViewModel = ViewModelProvider(requireActivity()).get(PostViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val audioFileName = UUID.randomUUID().toString() + ".aac"
        fileName = File(requireActivity().externalCacheDir, audioFileName).absolutePath
        requestPermissions()

        binding.btnrecord.setOnClickListener {
            toggleRecording()
        }
        binding.play.apply {
            text = "Ghi âm"
            setOnClickListener {
                toggleRecording()
            }
        }

        setupWaveformView()
        postAudio()

    }

    private fun toggleRecording() {
        if (!isRecording) {
            startRecording()
            binding.play.text = "Đang ghi âm"
        } else {
            stopRecording()
            binding.play.text = "phát"
        }

        binding.play.setOnClickListener {
            if (!isPlaying) {
                playAudio(fileName)
                binding.play.text = "dừng"
            } else {
                stopPlaying()
                binding.play.text = "phát"
            }
        }
    }

    private fun startRecording() {
        if (allPermissionsGranted()) {
            try {
                mediaRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioChannels(1)
                    setAudioSamplingRate(16000)
                    setAudioEncodingBitRate(64000)
                    setMaxDuration(RECORDING_MEDIA_RECORDER_MAX_DURATION)
                    setOutputFile(fileName)
                    prepare()
                    start()
                }
                isRecording = true

                startTimer()
            } catch (e: IOException) {
                Log.e("TAGY", "startRecording: ${e.message}")
            }
        } else {
            requestPermissions()
        }
    }


    private fun stopRecording() {
        mediaRecorder.apply {
            stop()
            release()
        }
        isRecording = false
        mediaRecorder = MediaRecorder()
        countDownTimer.cancel()
        binding.tvTimer.text = "00:00/$totalTimer"
        displayWaveform(fileName)
        Log.d("TAGY", "$fileName")
        binding.btnrecord.visibility = View.GONE
        binding.btnPost.visibility = View.VISIBLE
        binding.btnLeft.visibility = View.VISIBLE
    }



    //hien thi progress
    private val updateWaveform = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (isPlaying) {
                    val progress = (player.currentPosition.toFloat() / player.duration)*100
                    Log.d("TAGY","$progress , $player.duration")
                    binding.wave.progress = progress
                    handler.postDelayed(this, 1) // Update every 50ms for smooth animation
                }
            }
        }
    }
    //play
    private fun playAudio(audioFilePath: String) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFilePath)
                prepare()
                start()
            }
            isPlaying = true
            handler.post(updateWaveform)
            mediaPlayer.setOnCompletionListener {
                stopPlaying()

            }
        } catch (e: IOException) {
            Log.e("TAGY", "Error playing audio: ${e.message}")
        }
    }
    private fun stopPlaying() {
        mediaPlayer.stop()
        isPlaying = false
        binding.play.text = "Phát"
        handler.removeCallbacks(updateWaveform)
        binding.wave.progress = 0f

        mediaPlayer.reset()
        mediaPlayer.release()
    }
    override fun onStop() {
        super.onStop()

    }
    override fun onDestroy() {
        super.onDestroy()
            mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
    }
    private fun setupWaveformView() {
        binding.wave.onProgressListener = object : OnProgressListener {
            override fun onProgressChanged(progress: Float, byUser: Boolean) {
                Log.d("TAGY", "Progress set: $progress, and it's $byUser that user did this")
                if (byUser && isPlaying) {
                    val seekToPosition = (mediaPlayer!!.duration * progress / 100.0).toInt()
//                        postRowVoiceBinding.wave.progress = progress
                    Log.d("TAGY", "Seeking to position: $seekToPosition")
                    mediaPlayer?.seekTo(seekToPosition)
                    // Chờ đợi 100ms (có thể điều chỉnh tùy theo thiết bị)
//                        Handler(Looper.getMainLooper()).postDelayed({
//                            if(mediaPlayer.isPlaying){
//                                isplay =true
//
//                            }
//
//                        }, 100) // Khởi động lại Media Player ngay sau khi tua
                }
            }

            override fun onStartTracking(progress: Float) {
                Log.d("TAGY", "Started tracking from $progress")
            }

            override fun onStopTracking(progress: Float) {

                if(isPlaying){
                    Log.d("TAGY", "Stopped tracking at $progress")
                    val seekToPosition = (mediaPlayer!!.duration * (progress/100.0)).toInt()
                    Log.d("TAGY", "Seeking to position: $seekToPosition")
                    mediaPlayer?.seekTo(seekToPosition)
                }

            }
        }
    }

//dem xem bn s
    private fun startTimer(){
         countDownTimer = object : CountDownTimer(RECORDING_MEDIA_RECORDER_MAX_DURATION.toLong(),1000){
            override fun onTick(millisUntilFinished: Long) {
                val elapsedTime = RECORDING_MEDIA_RECORDER_MAX_DURATION - millisUntilFinished
                val seconds = elapsedTime  / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                val sd = millisUntilFinished/1000
                binding.tvTimer.text = String.format("%02d:%02d", minutes, sd%60)
                totalTimer = String.format("%02d:%02d", minutes, remainingSeconds)
            }

            override fun onFinish() {
                if (isRecording) {
                    stopRecording()
                    binding.play.text = "phát"
                }
            }

        }.start()
    }


    private fun displayWaveform(audioFilePath: String) {
        try {
            val inputStream = FileInputStream(audioFilePath)
            val audioData = inputStream.readBytes()

            binding.wave.setRawData(audioData)

            Log.d("TAGY", "Waveform ok")
            Log.d("TAGY", "$fileName")
        } catch (e: IOException) {
            Log.e("TAGY", "Error reading: ${e.message}")
        }
    }

    private fun postAudio() {
        binding.btnPost.setOnClickListener {
            val fileUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                File(fileName)
            )
            val content = binding.edt1.text.toString()
            postViewModel.addPost(fileUri, content, false)
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}
