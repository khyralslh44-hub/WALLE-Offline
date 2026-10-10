package com.example.walle

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class MainActivity : Activity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvEmotionText: TextView
    private lateinit var pbHappiness: ProgressBar
    private lateinit var ivWallEFace: ImageView
    private lateinit var btnTalk: Button
    private lateinit var labeler: ImageLabeler

    private var robotCuriosity = 50
    private val pickImageRequest = 4102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvEmotionText = findViewById(R.id.tvEmotionText)
        pbHappiness = findViewById(R.id.pbHappiness)
        ivWallEFace = findViewById(R.id.ivWallEFace)
        btnTalk = findViewById(R.id.btnTalk)

        pbHappiness.max = 100
        labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        btnTalk.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            @Suppress("DEPRECATION")
            startActivityForResult(
                Intent.createChooser(intent, "اختر صورة"),
                pickImageRequest
            )
        }

        updateRobotUI()
    }

    @Deprecated("استخدام واجهة اختيار الصور التقليدية للتوافق مع Activity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImageRequest && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                ivWallEFace.setImageURI(uri)
                val image = InputImage.fromFilePath(this, uri)
                analyzeLivingThingsAndFood(image)
            } catch (_: Exception) {
                tvEmotionText.text = "⚠️ تعذّر فتح الصورة. جرّب صورة أخرى."
            }
        }
    }

    private fun analyzeLivingThingsAndFood(inputImage: InputImage) {
        tvEmotionText.text = "👀 WALL-E يفحص الصورة..."
        btnTalk.isEnabled = false

        labeler.process(inputImage)
            .addOnSuccessListener { labels ->
                val topLabel = labels.maxByOrNull { it.confidence }

                if (topLabel == null) {
                    tvEmotionText.text = "🤖 لم أستطع تمييز شيء بوضوح."
                } else {
                    val detectedObject = topLabel.text.lowercase()
                    val confidence = (topLabel.confidence * 100).toInt()

                    when {
                        detectedObject.contains("cat") ||
                        detectedObject.contains("dog") ||
                        detectedObject.contains("animal") ||
                        detectedObject.contains("bird") ||
                        detectedObject.contains("horse") ||
                        detectedObject.contains("fish") ||
                        detectedObject.contains("rabbit") -> {
                            robotCuriosity = (robotCuriosity + 15).coerceAtMost(100)
                            tvEmotionText.text =
                                "🤖 بيب بيب! رصد WALL-E حيوانًا: $detectedObject\nالثقة: $confidence%\nإنه يراقبه بحذر وفضول!"
                            ivWallEFace.animate().scaleX(0.88f).scaleY(0.88f)
                                .rotation(-6f).setDuration(400).withEndAction {
                                    ivWallEFace.scaleX = 1f
                                    ivWallEFace.scaleY = 1f
                                    ivWallEFace.rotation = 0f
                                }.start()
                        }

                        detectedObject.contains("food") ||
                        detectedObject.contains("fruit") ||
                        detectedObject.contains("apple") ||
                        detectedObject.contains("banana") ||
                        detectedObject.contains("bread") ||
                        detectedObject.contains("pizza") ||
                        detectedObject.contains("vegetable") ||
                        detectedObject.contains("orange") -> {
                            robotCuriosity = (robotCuriosity + 10).coerceAtMost(100)
                            tvEmotionText.text =
                                "🤖 وااالي؟! 🍎 وجد طعامًا: $detectedObject\nالثقة: $confidence%\nWALL-E يتأمله بفضول!"
                            ivWallEFace.animate().scaleX(1.08f).scaleY(1.08f)
                                .rotation(6f).setDuration(300).withEndAction {
                                    ivWallEFace.scaleX = 1f
                                    ivWallEFace.scaleY = 1f
                                    ivWallEFace.rotation = 0f
                                }.start()
                        }

                        detectedObject.contains("person") ||
                        detectedObject.contains("people") ||
                        detectedObject.contains("human") ||
                        detectedObject.contains("face") -> {
                            robotCuriosity = (robotCuriosity + 10).coerceAtMost(100)
                            tvEmotionText.text =
                                "🥰 وااالي! يبدو أن هناك شخصًا في الصورة.\nالتصنيف: $detectedObject\nالثقة: $confidence%\nWALL-E يلوّح بسعادة!"
                            ivWallEFace.animate().scaleX(1.05f).scaleY(1.05f)
                                .setDuration(200).withEndAction {
                                    ivWallEFace.scaleX = 1f
                                    ivWallEFace.scaleY = 1f
                                }.start()
                        }

                        else -> {
                            robotCuriosity = (robotCuriosity + 5).coerceAtMost(100)
                            tvEmotionText.text =
                                "🤖 WALL-E يستكشف العالم!\nالتصنيف: $detectedObject\nالثقة: $confidence%"
                        }
                    }
                }

                updateRobotUI()
                btnTalk.isEnabled = true
            }
            .addOnFailureListener {
                tvEmotionText.text = "⚠️ حدث خطأ أثناء تحليل الصورة."
                btnTalk.isEnabled = true
            }
    }

    private fun updateRobotUI() {
        pbHappiness.progress = robotCuriosity
        tvStatus.text = "الحواس البصرية: جاهزة | مستوى الفضول: $robotCuriosity%"
    }

    override fun onDestroy() {
        if (::labeler.isInitialized) labeler.close()
        super.onDestroy()
    }
}
