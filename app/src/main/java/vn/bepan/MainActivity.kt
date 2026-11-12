package vn.bepan

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.*
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.bepan.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            BepTheme {
                val vm: KitchenViewModel = viewModel()
                var listening by remember { mutableStateOf(false) }
                var voiceStatus by remember { mutableStateOf("") }
                val recognizer = remember { if (SpeechRecognizer.isRecognitionAvailable(this)) SpeechRecognizer.createSpeechRecognizer(this) else null }
                DisposableEffect(recognizer) {
                    recognizer?.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) { listening = true; voiceStatus = "Đang nghe · bấm micro để dừng" }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() { listening = false; voiceStatus = "Đang nhận dạng…" }
                        override fun onError(error: Int) { listening = false; voiceStatus = "Chưa nhận dạng được. Bạn có thể nhập bằng chữ." }
                        override fun onResults(results: Bundle?) { listening = false; results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { vm.draft(it) }; voiceStatus = "Kiểm tra và sửa chữ trước khi gửi." }
                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                    onDispose { recognizer?.destroy() }
                }
                fun startVoice() {
                    if (recognizer == null) { voiceStatus = "Thiết bị chưa hỗ trợ nhận dạng. Bạn có thể nhập bằng chữ."; return }
                    recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                    })
                }
                val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) startVoice() else voiceStatus = "Chưa cấp quyền micro. Nhập chữ vẫn dùng được."
                }
                KitchenApp(vm, listening, voiceStatus) {
                    if (listening) { recognizer?.stopListening(); listening = false }
                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }
}
