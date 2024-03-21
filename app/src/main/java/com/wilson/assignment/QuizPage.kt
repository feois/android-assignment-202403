package com.wilson.assignment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_QUIZ_ID = "quiz_id"
private const val ARG_CONTENT_ID = "content_id"
private const val ARG_ANSWER = "answer"

/**
 * A simple [Fragment] subclass.
 * Use the [QuizPage.newInstance] factory method to
 * create an instance of this fragment.
 */
class QuizPage : Fragment() {
    private var quizId: Int = 0
    private var contentId: Int = 0

    private var count = 0
    private var editText: EditText? = null
    private var radioSelected = 0
    private val checked = mutableSetOf<Int>()

    private val question: Quiz.Question get() = Quizzes[quizId].contents[contentId]

    override fun onSaveInstanceState(outState: Bundle) {
        when (question.type) {
            0 -> {
                outState.putString(ARG_ANSWER, editText?.text.toString())
                editText = null
            }
            1 -> {
                outState.putInt(ARG_ANSWER, radioSelected)
            }
            else -> {
                outState.putIntArray(ARG_ANSWER, checked.toIntArray())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            quizId = it.getInt(ARG_QUIZ_ID)
            contentId = it.getInt(ARG_CONTENT_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_quiz_page, container, false)
        val linearLayout = view.findViewById<LinearLayout>(R.id.quizFragmentLinearLayout)
        val questionText = view.findViewById<TextView>(R.id.question)

        questionText.text = question.question

        when (question.type) {
            0 -> {
                inflater.inflate(R.layout.quiz_edit_text, linearLayout)

                editText = linearLayout.getChildAt(linearLayout.childCount - 1) as EditText

                editText?.inputType = question.answer_type

                savedInstanceState?.getString(ARG_ANSWER)?.let { s ->
                    editText?.text?.let {
                        it.clear()
                        it.append(s)
                    }
                }
            }
            1 -> {
                inflater.inflate(R.layout.quiz_radio_group, linearLayout)

                val radioGroup = linearLayout.getChildAt(linearLayout.childCount - 1) as RadioGroup

                for (answer in question.answers) {
                    inflater.inflate(R.layout.quiz_radio_button, radioGroup)

                    val index = radioGroup.childCount - 1
                    val radioButton = radioGroup.getChildAt(index) as RadioButton

                    radioButton.text = answer
                    radioButton.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            radioSelected = index
                        }
                    }
                }

                savedInstanceState?.let {
                    (radioGroup.getChildAt(it.getInt(ARG_ANSWER)) as RadioButton).isChecked = true
                }
            }
            else -> {
                for (answer in question.answers) {
                    inflater.inflate(R.layout.quiz_check_box, linearLayout)

                    val index = linearLayout.childCount - 1
                    val checkBox = linearLayout.getChildAt(index) as CheckBox

                    checkBox.text = answer
                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            if (question.type > 0 && count == question.type) {
                                buttonView.isChecked = false
                                Toast.makeText(context, "You cannot choose more than ${question.type}", Toast.LENGTH_SHORT).show()
                            }
                            else {
                                checked.add(index)
                                count++
                            }
                        }
                        else {
                            checked.remove(index)
                            count--
                        }
                    }
                }

                count = 0

                savedInstanceState?.getIntArray(ARG_ANSWER)?.forEach {
                    (linearLayout.getChildAt(it) as CheckBox).isChecked = true
                }
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
        fun newInstance(quizId: Int, contentId: Int) =
            QuizPage().apply {
                arguments = Bundle().apply {
                    putInt(ARG_QUIZ_ID, quizId)
                    putInt(ARG_CONTENT_ID, contentId)
                }
            }
    }
}