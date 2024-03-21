package com.wilson.assignment

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

data class Quiz(val name: String)

class QuizViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val root = view
    val title: TextView

    init {
        title = view.findViewById(R.id.quizName)
    }
}

class QuizListAdapter: RecyclerView.Adapter<QuizViewHolder>() {
    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        context = parent.context

        return QuizViewHolder(LayoutInflater.from(context).inflate(R.layout.quiz_item, parent, false))
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        holder.title.text = QuizzesFragment.quizzes[position].name
        holder.root.setOnClickListener {
            val intent = Intent(context, QuizActivity::class.java)

            context.startActivity(intent)
        }
    }

    override fun getItemCount() = QuizzesFragment.quizzes.size
}

/**
 * A simple [Fragment] subclass.
 */
class QuizzesFragment : Fragment() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
        = inflater.inflate(R.layout.fragment_quizzes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val quizList = view.findViewById<RecyclerView>(R.id.quizList)

        if (quizzes.isEmpty()) {
            initQuizzes(resources)
        }

        quizList.layoutManager = LinearLayoutManager(context)
        quizList.adapter = QuizListAdapter()
    }

    companion object {
        var quizzes: List<Quiz> = arrayListOf()

        fun initQuizzes(resources: Resources) {
            val json = resources.openRawResource(R.raw.quizzes).bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)

            quizzes = (0..<jsonArray.length()).map {
                val quiz = jsonArray.getJSONObject(it)

                Quiz(quiz.getString("name"))
            }.toList()
        }
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment QuizzesFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            QuizzesFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
    }
}