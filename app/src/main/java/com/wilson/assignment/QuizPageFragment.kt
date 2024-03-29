package com.wilson.assignment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_QUIZ_ID = "quiz_id"
private const val ARG_INDEXES = "shuffled_indexes"
private const val ARG_QUESTION_INDEX = "question_index"
private const val ARG_ANSWER = "answer"

/**
 * A simple [Fragment] subclass.
 * Use the [QuizPageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class QuizPageFragment : Fragment() {
    private var quizId = ""
    private var indexes = intArrayOf()
    private var questionIndex = 0

    private var checkedCount = 0
    private var editText: EditText? = null
    private var radioSelected = 0
    private val checked = mutableSetOf<Int>()

    private val quizCache: QuizCacheViewModel by activityViewModels()
    private val question by lazy { quizCache.quiz.value!!.questions[indexes[questionIndex]] }

    override fun onSaveInstanceState(outState: Bundle) {
        when (question) {
            is Quiz.SubjectiveQuestion -> {
                outState.putString(ARG_ANSWER, editText?.text.toString())
                editText = null
            }
            is Quiz.UniselectionalObjectiveQuestion -> {
                outState.putInt(ARG_ANSWER, radioSelected)
            }
            is Quiz.MultiselectionalObjectiveQuestion -> {
                outState.putIntArray(ARG_ANSWER, checked.toIntArray())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            quizId = it.getString(ARG_QUIZ_ID)!!
            indexes = it.getIntArray(ARG_INDEXES)!!
            questionIndex = it.getInt(ARG_QUESTION_INDEX)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_quiz_page, container, false)

        question.run {
            val linearLayout = view.findViewById<LinearLayout>(R.id.quizFragmentLinearLayout)
            val questionText = view.findViewById<TextView>(R.id.question)

            questionText.text = question

            when (this) {
                is Quiz.SubjectiveQuestion -> {
                    inflater.inflate(R.layout.quiz_edit_text, linearLayout)

                    editText = linearLayout.getChildAt(linearLayout.childCount - 1) as EditText

                    editText?.inputType = inputType

                    savedInstanceState?.getString(ARG_ANSWER)?.let { s ->
                        editText?.text?.let {
                            it.clear()
                            it.append(s)
                        }
                    }
                }

                is Quiz.UniselectionalObjectiveQuestion -> {
                    inflater.inflate(R.layout.quiz_radio_group, linearLayout)

                    val radioGroup =
                        linearLayout.getChildAt(linearLayout.childCount - 1) as RadioGroup

                    for (option in options) {
                        inflater.inflate(R.layout.quiz_radio_button, radioGroup)

                        val index = radioGroup.childCount - 1
                        val radioButton = radioGroup.getChildAt(index) as RadioButton

                        radioButton.text = option
                        radioButton.setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                radioSelected = index
                            }
                        }
                    }

                    savedInstanceState?.let {
                        (radioGroup.getChildAt(it.getInt(ARG_ANSWER)) as RadioButton).isChecked =
                            true
                    }
                }

                is Quiz.MultiselectionalObjectiveQuestion -> {
                    for (option in options) {
                        inflater.inflate(R.layout.quiz_check_box, linearLayout)

                        val index = linearLayout.childCount - 1
                        val checkBox = linearLayout.getChildAt(index) as CheckBox

                        checkBox.text = option
                        checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                            if (!isChecked) {
                                checked.remove(index)
                                checkedCount--
                            }
                            else if (hint && checkedCount == answers.size) {
                                buttonView.isChecked = false
                                context?.shortToast("You cannot choose more than ${options.size}")
                            }
                            else {
                                checked.add(index)
                                checkedCount++
                            }
                        }
                    }

                    checkedCount = 0

                    savedInstanceState?.getIntArray(ARG_ANSWER)?.forEach {
                        (linearLayout.getChildAt(it) as CheckBox).isChecked = true
                    }
                }

                else -> {}
            }
        }

        return view
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
        fun newInstance(quizId: String, indexes: IntArray, questionIndex: Int) =
            QuizPageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QUIZ_ID, quizId)
                    putIntArray(ARG_INDEXES, indexes)
                    putInt(ARG_QUESTION_INDEX, questionIndex)
                }
            }
    }
}