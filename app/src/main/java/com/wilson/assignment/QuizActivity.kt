package com.wilson.assignment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.Timestamp
import java.util.Date

class ResultViewModel: ViewModel() {
    var result = booleanArrayOf()
    var blanks = mutableSetOf<Int>()
    val requestResult = MutableLiveData<Unit>()
    val resultRequestNotifier = MutableLiveData<Unit>()
    val requestBlank = MutableLiveData<Int>()
    val blankRequestNotifier = MutableLiveData<Unit>()
}

class QuizCacheViewModel: ViewModel() {
    internal val quizLiveData = MutableLiveData<Quiz>()
    val quiz: LiveData<Quiz> get() = quizLiveData
}

class QuizActivity : AppCompatActivity() {
    private val userViewModel: UserViewModel by viewModels()
    private val quizCache: QuizCacheViewModel by viewModels()
    private val resultViewModel: ResultViewModel by viewModels()

    private var isWarning = false
    private var isExiting = false

    private val submitDialogBuilder by lazy {
        AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("Submitting...")
                .setMessage("")
    }
    private var submitDialog: AlertDialog? = null

    companion object {
        const val INTENT_QUIZ_ID = "quiz_id"
        const val INTENT_USERNAME = "username"
        const val INTENT_PASSWORD = "password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quiz)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val quizId = intent.getStringExtra(INTENT_QUIZ_ID)
        val username = intent.getStringExtra(INTENT_USERNAME)
        val password = intent.getStringExtra(INTENT_PASSWORD)

        if (username != null && password != null) {
            userViewModel.login(username, password)
        }

        if (quizId == null) {
            errorToast("Quiz Activity", IllegalArgumentException("No quiz id provided"))
        }
        else {
            Quiz.getQuiz(quizId).addOnCompleteListener {
                (it.exception ?: it.result?.exceptionOrNull())?.run {
                    errorToast("Failed to retrieve quiz $quizId", this)
                }

                initQuiz(it.result?.getOrNull())
            }
        }
    }

    private fun initQuiz(quiz: Quiz?) = quiz?.run {
        val pager = findViewById<ViewPager2>(R.id.quizPager)
        val prevQuiz = findViewById<ImageButton>(R.id.prevQuiz)
        val nextQuiz = findViewById<ImageButton>(R.id.nextQuiz)
        val submitQuiz = findViewById<Button>(R.id.submit)

        val count = questions.size
        val shuffledIndexes = if (allowReorder) { questions.indices.shuffled().toIntArray() }
                else { IntArray(questions.size).apply { forEachIndexed { i, _ -> this[i] = i } } }
        val quizPages = questions.indices.map { QuestionFragment.newInstance(id, shuffledIndexes, it) }

        quizCache.quizLiveData.value = this
        resultViewModel.blanks = questions.indices.toMutableSet()
        resultViewModel.result = BooleanArray(questions.size)

        pager.adapter = object : FragmentStateAdapter(this@QuizActivity) {
            override fun getItemCount() = count
            override fun createFragment(position: Int) = quizPages[position]
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                prevQuiz.isEnabled = position > 0
                nextQuiz.isEnabled = position + 1 < count
                submitQuiz.isEnabled = !nextQuiz.isEnabled
                isWarning = false
            }
        })

        resultViewModel.blankRequestNotifier.observe(this@QuizActivity) {
            resultViewModel.requestBlank.value = null

            if (isWarning) {
                if (resultViewModel.blanks.contains(pager.currentItem)) {
                    longToast("You haven't answered yet! Press again to skip")
                }
                else {
                    nextQuiz.callOnClick()
                }
            }
        }

        resultViewModel.resultRequestNotifier.observe(this@QuizActivity) {
            resultViewModel.requestResult.value = null

            submitDialog?.dismiss()

            if (resultViewModel.blanks.isNotEmpty()) {
                AlertDialog.Builder(this@QuizActivity)
                        .setTitle("Some questions are unanswered")
                        .setMessage("Do you still want to submit?")
                        .setPositiveButton("Yes") { _, _ ->
                            submit()
                        }
                        .setNegativeButton("No") { _, _ ->
                            pager.currentItem = resultViewModel.blanks.first()
                        }
                        .show()
            }
            else {
                submit()
            }
        }

        prevQuiz.setOnClickListener { pager.currentItem -= 1 }
        prevQuiz.setOnLongClickListener { pager.currentItem = 0; true }
        nextQuiz.setOnClickListener {
            isWarning = !isWarning

            if (isWarning) {
                resultViewModel.requestBlank.value = pager.currentItem
            }
            else {
                pager.currentItem += 1
            }
        }
        nextQuiz.setOnLongClickListener { pager.currentItem = count - 1; true }
        submitQuiz.setOnClickListener {
            submitDialog = submitDialogBuilder.show()
            resultViewModel.requestResult.value = Unit
        }

        onBackPressedDispatcher.addCallback(this@QuizActivity) {
            AlertDialog.Builder(this@QuizActivity)
                    .setTitle("Exit")
                    .setMessage("Are you sure you want to exit? Your progress won't be saved!")
                    .setPositiveButton("Yes") { _, _ -> finish() }
                    .setNegativeButton("No") { _, _ -> }
                    .show()
        }
    }

    private fun submit() {
        quizCache.quiz.value?.run {
            val marks = calculateMarks(resultViewModel.result)
            val intent = Intent(this@QuizActivity, ResultActivity::class.java)

            userViewModel.user.value?.apply {
                results.put(id, marks)
                userViewModel.updateUser()
                intent.putExtra(ResultActivity.INTENT_USERNAME, username)

                db.collection("items").document().set(mapOf(
                    "username" to username,
                    "quizId" to id,
                    "time" to Timestamp(Date()),
                ))
            }

            intent.putExtra(ResultActivity.INTENT_QUID_ID, id)
            intent.putExtra(ResultActivity.INTENT_RESULT, resultViewModel.result)

            startActivity(intent)
            finish()
        }
    }
}