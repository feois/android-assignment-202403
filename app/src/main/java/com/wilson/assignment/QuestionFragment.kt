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
 * Use the [QuestionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class QuestionFragment : Fragment() {
    private var quizId = ""
    private var indexes = intArrayOf()
    private var questionIndex = 0

    private lateinit var layoutInflater: LayoutInflater

    private var checkedCount = 0
    private var radioSelected = -1
    private val checked = mutableSetOf<Int>()
    private lateinit var editText: EditText

    private var blankCallback: () -> Boolean = { true }
    private var resultCallback: () -> Boolean = { true }

    private val quizCache: QuizCacheViewModel by activityViewModels()
    private val resultViewModel: ResultViewModel by activityViewModels()
    private val question by lazy { quizCache.quiz.value!!.questions[indexes[questionIndex]] }

    override fun onSaveInstanceState(outState: Bundle) {
        checkBlank()
        checkResult()

        when (question) {
            is Quiz.SubjectiveQuestion -> {
                outState.putString(ARG_ANSWER, editText.text.toString())
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
    ): View? = inflater.run { layoutInflater = this; inflate(R.layout.fragment_question, container, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = question.run {
        val linearLayout = view.findViewById<LinearLayout>(R.id.quizFragmentLinearLayout)
        val questionText = view.findViewById<TextView>(R.id.question)
        val optionsCount = view.findViewById<TextView>(R.id.optionsCount)
        val marksText = view.findViewById<TextView>(R.id.marks)

        questionText.text = question
        marksText.text = "Marks: $marks"
        optionsCount.visibility = View.GONE

        when (this) {
            is Quiz.SubjectiveQuestion -> {
                layoutInflater.inflate(R.layout.quiz_edit_text, linearLayout)

                editText = linearLayout.getChildAt(linearLayout.childCount - 1) as EditText

                editText.inputType = inputType

                savedInstanceState?.getString(ARG_ANSWER)?.let { s ->
                    editText.text?.let {
                        it.clear()
                        it.append(s)
                    }
                }

                blankCallback = { editText.text.isEmpty() }
                resultCallback = { answer == null || answer == editText.text?.toString() }
            }

            is Quiz.UniselectionalObjectiveQuestion -> {
                layoutInflater.inflate(R.layout.quiz_radio_group, linearLayout)

                val radioGroup =
                    linearLayout.getChildAt(linearLayout.childCount - 1) as RadioGroup

                val shuffle = (0..<options.size).run { if (allowReorder) { shuffled() } else { toList() } }
                val map = shuffle.withIndex().associate { Pair(it.value, it.index) }

                for (i in 0..<options.size) {
                    layoutInflater.inflate(R.layout.quiz_radio_button, radioGroup)

                    val radioButton = radioGroup.getChildAt(i) as RadioButton

                    radioButton.text = options[shuffle[i]]
                    radioButton.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            radioSelected = shuffle[i]
                        }
                    }
                }

                savedInstanceState?.let {
                    val index = it.getInt(ARG_ANSWER, -1)

                    if (index != -1) {
                        (radioGroup.getChildAt(map[index]!!) as RadioButton).isChecked = true
                    }
                }

                blankCallback = { radioSelected == -1 }
                resultCallback = { answer == radioSelected }
            }

            is Quiz.MultiselectionalObjectiveQuestion -> {
                val childCount = linearLayout.childCount
                val shuffle = (0..<options.size).run { if (allowReorder) { shuffled() } else { toList() } }
                val map = shuffle.withIndex().associate { Pair(it.value, it.index) }

                for (i in 0..<options.size) {
                    layoutInflater.inflate(R.layout.quiz_check_box, linearLayout)

                    val checkBox = linearLayout.getChildAt(i + childCount) as CheckBox
                    val index = shuffle[i]

                    checkBox.text = options[index]
                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (!isChecked) {
                            checked.remove(index)
                            checkedCount--
                        }
                        else if (hint && checkedCount == answers.size) {
                            buttonView.isChecked = false
                            context?.shortToast("You cannot choose more than ${answers.size}")
                        }
                        else {
                            checked.add(index)
                            checkedCount++
                        }
                    }
                }

                if (hint) {
                    optionsCount.visibility = View.VISIBLE
                    optionsCount.text = "Select ${answers.size} answers"
                }

                checkedCount = 0

                savedInstanceState?.getIntArray(ARG_ANSWER)?.forEach {
                    (linearLayout.getChildAt(childCount + map[it]!!) as CheckBox).isChecked = true
                }

                blankCallback = { hint && checked.size != answers.size }
                resultCallback = {
                    answers.size == checked.size && answers.all { checked.contains(it) } }
            }

            else -> {}
        }

        resultViewModel.requestBlank.observe(viewLifecycleOwner) {
            if (it == questionIndex) {
                checkBlank()
                resultViewModel.blankRequestNotifier.value = Unit
            }
        }

        resultViewModel.requestResult.observe(viewLifecycleOwner) {
            if (it != null) {
                checkBlank()
                checkResult()

                if (questionIndex + 1 == quizCache.quiz.value?.questions?.size) {
                    resultViewModel.resultRequestNotifier.value = Unit
                }
            }
        }
    }

    private fun checkBlank() = resultViewModel.blanks.run {
        if (blankCallback()) {
            add(questionIndex)
            true
        }
        else {
            remove(questionIndex)
            false
        }
    }

    private fun checkResult() {
        resultViewModel.result[indexes[questionIndex]] = resultCallback()
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
            QuestionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QUIZ_ID, quizId)
                    putIntArray(ARG_INDEXES, indexes)
                    putInt(ARG_QUESTION_INDEX, questionIndex)
                }
            }
    }
}