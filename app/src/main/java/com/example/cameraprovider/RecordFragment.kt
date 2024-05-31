package com.example.cameraprovider

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.cameraprovider.databinding.FragmentRecordBinding
import java.io.FileInputStream
import java.io.IOException
import android.media.MediaCodecList

class RecordFragment : Fragment() {

    lateinit var binding: FragmentRecordBinding

    var isRecording: Boolean = false
    var isplay: Boolean = false
    private var fileName: String = ""
    lateinit var mediaRecorder: MediaRecorder

    lateinit var mediaPlayer: MediaPlayer
   private val RECORDING_MEDIA_RECORDER_MAX_DURATION =240000
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in RecordFragment.REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    context,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                isRecording = false
                isplay = false
                // Record to the external cache directory for visibility

            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_record, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileName = "${requireActivity().externalCacheDir?.absolutePath}/audiorecordtest.3gp"
        // Yêu cầu quyền ghi âm khi Fragment được tạo
        requestPermissions()
        // Đăng ký sự kiện click cho nút ghi âm
        binding.btnrecord.setOnClickListener {
            toggleRecording()
        }
        binding.play.text = "Ghi âm"

    }

    private fun toggleRecording() {
        if (!isRecording) {
            startRecording()
            binding.play.text = "Đang ghi âm"
        } else {

            stopRecording()
        }


        binding.play.apply {
            setOnClickListener {
                if (!isplay) {
                    playAudio(fileName)
                    text = "dừng"
                } else {
                    stopPlaying()
                    text = "phát"
                }
            }
        }


    }

    private fun playAudio(audioFilePath: String) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFilePath)
                prepare()
                start()
            }
            isplay = true
        } catch (e: IOException) {
            Log.e("RecordFragment", "Error playing audio: ${e.message}")
        }
    }

    private fun stopPlaying() {
        mediaPlayer.release()

        isplay = false
    }


    fun isHeAacEncoderSupported(): Boolean {
        val codecCount = MediaCodecList.getCodecCount()
        for (i in 0 until codecCount) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (codecInfo.isEncoder && codecInfo.name.startsWith("OMX.google") && codecInfo.supportedTypes.any { it.equals("audio/mp4a-latm", ignoreCase = true) }) {
                return true
            }
        }
        return false
    }
    private fun startRecording() {
        if (allPermissionsGranted()) {
            try {
                mediaRecorder = MediaRecorder().apply {
                    //mic đt
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    //định dạng mpeg_4
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    //chi 1kenh
                    setAudioChannels(1)
                    //rate
                    setAudioSamplingRate(8000)
                    setAudioEncodingBitRate(32000)
                    //bitrate
                    setMaxDuration(RECORDING_MEDIA_RECORDER_MAX_DURATION)
                    //adr4.3
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        if (isHeAacEncoderSupported()) {
                            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
                        } else {
                           setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        }
                    } else {
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    }
                    setOutputFile(fileName)
                    prepare()
                    start()
                }
                isRecording = true
                binding.play.text = "Đang ghi âm"
            } catch (e: IOException) {
                Log.e(TAG, "startRecording: ${e.message}")
            }
        } else {
            requestPermissions()
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
        }
        isRecording = false

        binding.play.text = "Phát"


        // Tiến hành hiển thị sóng âm trên AudioWaveView
        displayWaveform(fileName)
    }

    private fun displayWaveform(audioFilePath: String) {
        try {
            val inputStream = FileInputStream(audioFilePath)
            val audioData = inputStream.readBytes()
            binding.wave.setRawData(audioData) {
                // Xử lý sau khi dữ liệu âm thanh được thiết lập và sóng âm được hiển thị
                Log.d("RecordFragment", "Waveform displayed successfully")
            }
        } catch (e: IOException) {
            Log.e("RecordFragment", "Error reading audio file: ${e.message}")
        }
    }

    private fun requestPermissions() {
        //goi cai giao dien ma hoi cap quyen ?
        activityResultLauncher.launch(RecordFragment.REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = RecordFragment.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireActivity().baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {

        private const val TAG = "Record"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.RECORD_AUDIO,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}