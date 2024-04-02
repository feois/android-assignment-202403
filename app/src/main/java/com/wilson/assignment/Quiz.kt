package com.wilson.assignment

import android.text.InputType
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.lang.IllegalArgumentException

data class Quiz(val id: String, val name: String, val allowReorder: Boolean, val questions: List<Question>) {
    companion object {
        val collection get() = db.collection("quizzes")

        fun getQuiz(id: String) = collection.document(id).get().continueWith {
            it.runCatching {
                require(isSuccessful) { "Failed to retrieve quiz $id" }
                require(result.exists()) { "Quiz $id does not exist" }

                result.data!!.toQuiz(id).getOrThrow()
            }
        }
    }

    val totalMarks by lazy { questions.sumOf { it.marks } }

    abstract class Question {
        abstract val question: String
        abstract val marks: Int
    }

    abstract class ObjectiveQuestion: Question() {
        abstract val allowReorder: Boolean
        abstract val options: List<String>
    }

    data class SubjectiveQuestion(override val question: String,
                                                       override val marks: Int,
                                                       val type: String,
                                                       val answer: String? = null): Question() {
        val inputType by lazy {
            when (type) {
                "integer" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                "decimal" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                "text" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                "long text" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                else -> InputType.TYPE_CLASS_TEXT
            }
        }
    }
    data class UniselectionalObjectiveQuestion(override val question: String,
                                               override val marks: Int,
                                               val answer: Int,
                                               override val options: List<String>,
                                               override val allowReorder: Boolean = true): ObjectiveQuestion()
    data class MultiselectionalObjectiveQuestion(override val question: String = "",
                                                 override val marks: Int,
                                                 val answers: List<Int> = listOf(),
                                                 val hint: Boolean = true,
                                                 override val options: List<String>,
                                                 override val allowReorder: Boolean = true): ObjectiveQuestion()
}

fun FirestoreMap.toQuiz(id: String) = runCatching {
    Quiz(
        id,
        getProp("name"),
        getOrDefault("reorder", true) as Boolean,
        getListProp<Map<String, *>>("questions").map { map ->
            map.run {
                val question = getProp<String>("question")
                val marks = getIntProp("marks")

                when {
                    containsKey("type") -> Quiz.SubjectiveQuestion(
                        question,
                        marks,
                        getProp("type"),
                        get("answer") as? String,
                    )

                    containsKey("answer") -> Quiz.UniselectionalObjectiveQuestion(
                        question,
                        marks,
                        getIntProp("answer"),
                        getListProp<String>("options"),
                        getOrDefault("reorder", true) as Boolean,
                    )

                    containsKey("answers") -> Quiz.MultiselectionalObjectiveQuestion(
                        question,
                        marks,
                        getListProp<Long>("answers").map { it.toInt() },
                        getOrDefault("hint", false) as Boolean,
                        getListProp<String>("options"),
                        getOrDefault("reorder", true) as Boolean,
                    )

                    else -> throw IllegalArgumentException("Invalid question format")
                }
            }
        }
    )
}

class QuizViewModel: ViewModel() {
    private val quizzesLiveData = MutableLiveData<List<Quiz>>()
    val quizzes: LiveData<List<Quiz>> get() = quizzesLiveData

    fun updateQuizzes() = Quiz.collection.get().continueWith { task ->
        task.runCatching {
            require(isSuccessful) { "Failed to retrieve quizzes" }

            val quizList = mutableListOf<Quiz>()

            result.documents.forEach {
                it.data?.toQuiz(it.id)?.getOrThrow()?.run {
                    quizList.add(this)
                    quizzesLiveData.value = quizList
                }
            }
        }
    }
}
