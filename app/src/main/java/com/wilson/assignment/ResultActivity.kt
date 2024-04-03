package com.wilson.assignment

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.OutputStream

class QuestionViewHolder(val view: View): RecyclerView.ViewHolder(view) {
    private val expand = view.findViewById<ImageView>(R.id.questionItemExpand)
    private val questionText = view.findViewById<TextView>(R.id.questionItemQuestion)
    private val status = view.findViewById<TextView>(R.id.questionItemStatus)

    fun initQuestion(question: Quiz.Question, result: Boolean) {
        questionText.text = question.question

        if (result) {
            view.setBackgroundColor(0x2600FF00)
            status.text = "✅"
        }
        else {
            view.setBackgroundColor(0x26FF0000)
            status.text = "❌"
        }

        view.setOnClickListener {
//            expand.startAnimation(RotateAnimation(0f, 180f,
//                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
//                    .apply { duration = 500 })
        }
    }
}

class ResultActivity : AppCompatActivity() {
    companion object {
        const val INTENT_USERNAME = "username"
        const val INTENT_QUID_ID = "quiz_id"
        const val INTENT_RESULT = "result"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_result)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val user = intent.getStringExtra(INTENT_USERNAME)?.run { User.getUser(this) }
        val quiz = intent.getStringExtra(INTENT_QUID_ID)?.run { Quiz.getQuiz(this) }
        val result = intent.getBooleanArrayExtra(INTENT_RESULT)

        runCatching {
            require(quiz != null) { "No quiz id provided" }
            require(result != null) { "No quiz result provided" }

            quiz.addOnSuccessListener { r ->
                r.fold({
                    initQuiz(it, result)
                }) {
                    errorToast("Cannot retrieve quiz", it)
                }
            }
        }.exceptionOrNull()?.run { errorToast("", this) }

        findViewById<ImageButton>(R.id.closeResult).setOnClickListener {
            finish()
        }
    }

    private fun initQuiz(quiz: Quiz, result: BooleanArray) {
        val marks = quiz.calculateMarks(result)

        findViewById<TextView>(R.id.textView).text = "Marks: $marks/${quiz.totalMarks}"

        findViewById<ImageButton>(R.id.share).setOnClickListener {
            val bitmap = it.rootView.run {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    draw(Canvas(this))
                }
            }
            val uri = saveImage(bitmap)

            if (uri != null) {
                startActivity(Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_SUBJECT, "Quiz ${quiz.name} result")
                    putExtra(Intent.EXTRA_TEXT, "I have completed quiz ${quiz.name}!")
                    putExtra(Intent.EXTRA_STREAM, uri)
                })
            }
        }

        val recycler = findViewById<RecyclerView>(R.id.questionList)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = object: RecyclerView.Adapter<QuestionViewHolder>() {
            override fun getItemCount() = quiz.questions.size

            override fun onBindViewHolder(holder: QuestionViewHolder, position: Int)
                = holder.initQuestion(quiz.questions[position], result[position])

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = QuestionViewHolder(
                LayoutInflater.from(this@ResultActivity).inflate(R.layout.question_item, parent, false)
            )
        }
    }

    // https://stackoverflow.com/a/66817176
    private fun saveImage(bitmap: Bitmap): Uri? {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream?
        val imageUri: Uri?
        val contentValues = ContentValues().apply {
            put(MediaColumns.DISPLAY_NAME, filename)
            put(MediaColumns.MIME_TYPE, "image/jpg")
            put(MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(Video.Media.IS_PENDING, 1)
        }

        //use application context to get contentResolver
        val contentResolver = application.contentResolver

        contentResolver.also { resolver ->
            imageUri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        }

        fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it) }

        contentValues.clear()
        contentValues.put(Video.Media.IS_PENDING, 0)
        if (imageUri != null) {
            contentResolver.update(imageUri, contentValues, null, null)
        }

        return imageUri
    }
}