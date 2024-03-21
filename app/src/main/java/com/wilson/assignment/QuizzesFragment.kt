package com.wilson.assignment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class QuizViewHolder(view: View): RecyclerView.ViewHolder(view) {
    private val root = view
    private val title: TextView

    init {
        title = view.findViewById(R.id.quizName)
    }

    fun initView(context: Context, quizId: Int) {
        val quiz = Quizzes[quizId]

        title.text = quiz.name
        root.setOnClickListener {
            val intent = Intent(context, QuizActivity::class.java)

            intent.putExtra(QuizActivity.INTENT_QUIZ_ID, quizId)
            context.startActivity(intent)
        }
    }
}

class QuizListAdapter: RecyclerView.Adapter<QuizViewHolder>() {
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        context = parent.context

        return QuizViewHolder(LayoutInflater.from(context).inflate(R.layout.quiz_item, parent, false))
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        holder.initView(context, position)
    }

    override fun getItemCount() = Quizzes.size
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

        quizList.layoutManager = LinearLayoutManager(context)
        quizList.adapter = QuizListAdapter()
    }

    companion object {
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