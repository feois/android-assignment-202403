package com.wilson.assignment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_QUIZ_ID = "quiz_id"

/**
 * A simple [Fragment] subclass.
 * Use the [QuizPage.newInstance] factory method to
 * create an instance of this fragment.
 */
class QuizPage : Fragment() {
    private var quizId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            quizId = it.getInt(ARG_QUIZ_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_quiz_page, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param quizId Quiz ID
         * @return A new instance of fragment QuizPage.
         */
        @JvmStatic
        fun newInstance(quizId: Int) =
            QuizPage().apply {
                arguments = Bundle().apply {
                    putInt(ARG_QUIZ_ID, quizId)
                }
            }
    }
}