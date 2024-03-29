package com.wilson.assignment

import android.text.InputType
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.lang.IllegalArgumentException

data class Quiz internal constructor(val id: String, val name: String, val allowReorder: Boolean, val questions: List<Question>) {
    abstract class Question {
        abstract val question: String
    }

    abstract class ObjectiveQuestion: Question() {
        abstract val allowReorder: Boolean
        abstract val options: List<String>
    }

    data class SubjectiveQuestion internal constructor(override val question: String,
                                                       val type: String,
                                                       val answer: String? = null): Question() {
        val inputType get() = when (type) {
            "integer" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            "decimal" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            "text" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            "long text" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            else -> InputType.TYPE_CLASS_TEXT
        }
    }
    data class UniselectionalObjectiveQuestion(override val question: String,
                                               val answer: Int,
                                               override val options: List<String>,
                                               override val allowReorder: Boolean = true): ObjectiveQuestion()
    data class MultiselectionalObjectiveQuestion(override val question: String = "",
                                                 val answers: List<Int> = listOf(),
                                                 val hint: Boolean = true,
                                                 override val options: List<String>,
                                                 override val allowReorder: Boolean = true): ObjectiveQuestion()
}

class QuizViewModel: ViewModel() {
    private val quizzesLiveData = MutableLiveData<List<Quiz>>()
    val quizzes: LiveData<List<Quiz>> get() = quizzesLiveData
    private val quizzesMapLiveData = MutableLiveData<Map<String, Quiz>>()
    val quizzesMap: LiveData<Map<String, Quiz>> get() = quizzesMapLiveData

    val collection get() = Firebase.firestore.collection("quizzes")

    fun fromMap(id: String, map: Map<String, *>) = map.runCatching {
        Quiz(
            id,
            getProp("name"),
            getOrDefault("reorder", true) as Boolean,
            getListProp<Map<String, *>>("questions").map { map ->
                map.run {
                    val question = getProp<String>("question")

                    when {
                        containsKey("type") -> Quiz.SubjectiveQuestion(
                            question,
                            getProp("type"),
                            get("answer") as? String,
                        )

                        containsKey("answer") -> Quiz.UniselectionalObjectiveQuestion(
                            question,
                            getProp<Long>("answer").toInt(),
                            getListProp<String>("options"),
                            getOrDefault("reorder", true) as Boolean,
                        )

                        containsKey("answers") -> Quiz.MultiselectionalObjectiveQuestion(
                            question,
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

    fun getQuiz(id: String) = collection.document(id).get().continueWith {
        it.runCatching {
            require(isSuccessful) { "Failed to retrieve quiz $id" }
            require(result.exists()) { "Quiz $id does not exist" }

            fromMap(id, result.data!!).getOrThrow()
        }
    }

    fun updateQuizzes() = collection.get().continueWith { task ->
        task.runCatching {
            require(isSuccessful) { "Failed to retrieve quizzes" }

            quizzesLiveData.value = result.documents.mapNotNull {
                it.data?.let { map -> fromMap(it.id, map).getOrThrow() }
            }
            quizzesMapLiveData.value = quizzes.value?.associateBy { it.id }
        }
    }
}
