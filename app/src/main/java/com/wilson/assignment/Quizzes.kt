package com.wilson.assignment

import android.content.res.Resources
import android.text.InputType
import org.json.JSONArray

data class Quiz(val id: Int, val name: String, val contents: List<Question>) {
    data class Question(val id: Int, val question: String, val type: Int, val options: List<String>, val answer_type: Int)
}

lateinit var Quizzes: List<Quiz> private set

fun initQuizzes(resources: Resources) {
    val json = resources.openRawResource(R.raw.quizzes).bufferedReader().use { it.readText() }
    val jsonArray = JSONArray(json)
    val list = arrayListOf<Quiz>()

    for (i in 0..<jsonArray.length()) {
        val quiz = jsonArray.getJSONObject(i)
        val name = quiz.getString("name")
        val contents = arrayListOf<Quiz.Question>()
        val contentArray = quiz.getJSONArray("contents")

        for (j in 0..<contentArray.length()) {
            val content = contentArray.getJSONObject(j)
            val question = content.getString("question")
            val type = content.getInt("type")
            val options = arrayListOf<String>()
            var answer_type = 0

            if (type == 0) {
                answer_type = when (content.get("hint")) {
                    "integer" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                    "decimal" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                    "text" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    "long text" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    else -> InputType.TYPE_CLASS_TEXT
                }
            }
            else {
                val optionArray = content.getJSONArray("options")

                for (k in 0..<optionArray.length()) {
                    options.add(optionArray.getString(k))
                }
            }

            contents.add(Quiz.Question(j, question, type, options, answer_type))
        }

        list.add(Quiz(i, name, contents))
    }

    Quizzes = list
}