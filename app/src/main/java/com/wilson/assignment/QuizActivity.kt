package com.wilson.assignment

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class QuizCacheViewModel: ViewModel() {
    internal val quizLiveData = MutableLiveData<Quiz>()
    val quiz: LiveData<Quiz> get() = quizLiveData
}

class QuizActivity : AppCompatActivity() {
    private val quizViewModel: QuizViewModel by viewModels()
    private val quizCache: QuizCacheViewModel by viewModels()

    companion object {
        const val INTENT_QUIZ_ID = "quiz_id"
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

        if (quizId == null) {
            errorToast("Quiz Activity", IllegalArgumentException("No quiz id provided"))
        }
        else {
            quizViewModel.getQuiz(quizId).addOnCompleteListener {
                (it.exception ?: it.result?.exceptionOrNull())?.run {
                    errorToast("Failed to retrieve quiz $quizId", this)
                }

                if (it.isSuccessful && it.result.isSuccess) {
                    val pager = findViewById<ViewPager2>(R.id.quizPager)
                    val prevQuiz = findViewById<ImageButton>(R.id.prevQuiz)
                    val nextQuiz = findViewById<ImageButton>(R.id.nextQuiz)

                    val quizPages = arrayListOf<Fragment>()

                    val quiz = it.result.getOrNull()!!
                    val shuffledIndexes = IntArray(quiz.questions.size).apply {
                        forEachIndexed { index, i ->
                            this[index] = index
                        }
                    }

                    quizCache.quizLiveData.value = quiz

                    if (quiz.allowReorder) {
                        shuffledIndexes.shuffle()
                    }

                    for (i in 0..quiz.questions.size) {
                        quizPages.add(QuizPageFragment.newInstance(quiz.id, shuffledIndexes, i))
                    }

                    pager.adapter = object : FragmentStateAdapter(this) {
                        override fun getItemCount() = quiz.questions.size
                        override fun createFragment(position: Int) = quizPages[position]
                    }

                    pager.registerOnPageChangeCallback(object :
                        ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            prevQuiz.isEnabled = position > 0
                            nextQuiz.isEnabled = position + 1 < pager.adapter!!.itemCount
                        }
                    })

                    prevQuiz.setOnClickListener { pager.currentItem -= 1 }
                    nextQuiz.setOnClickListener { pager.currentItem += 1 }
                }
            }
        }
    }
}