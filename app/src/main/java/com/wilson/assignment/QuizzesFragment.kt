package com.wilson.assignment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class QuizViewHolder(view: View): RecyclerView.ViewHolder(view) {
    private val root = view
    private val title: TextView

    init {
        title = view.findViewById(R.id.quizName)
    }

    fun initView(context: Context, quiz: Quiz) {
        title.text = quiz.name
        root.setOnClickListener {
            val intent = Intent(context, QuizActivity::class.java)

            intent.putExtra(QuizActivity.INTENT_QUIZ_ID, quiz.id)
            context.startActivity(intent)
        }
    }
}

class QuizListAdapter(private val context: Context, private val quizList: List<Quiz>): RecyclerView.Adapter<QuizViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        return QuizViewHolder(LayoutInflater.from(context).inflate(R.layout.quiz_item, parent, false))
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        holder.initView(context, quizList[position])
    }

    override fun getItemCount() = quizList.size
}

/**
 * A simple [Fragment] subclass.
 */
class QuizzesFragment : Fragment() {
    private val quizViewModel: QuizViewModel by activityViewModels()

    private lateinit var quizList: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_quizzes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val search = view.findViewById<SearchView>(R.id.search)
        val refresh = view.findViewById<ImageButton>(R.id.refreshQuizzes)

        quizList = view.findViewById(R.id.quizList)
        quizList.layoutManager = LinearLayoutManager(context)

        quizViewModel.quizzes.observe(viewLifecycleOwner) {
            quizList.adapter = QuizListAdapter(requireContext(), it)
        }

        refresh.setOnClickListener { button ->
            button.animation = RotateAnimation(0f, 360f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
                    .apply {
                        repeatCount = -1
                        duration = 2000
                    }

            quizViewModel.updateQuizzes().addOnCompleteListener {
                if (it.isSuccessful && it.result.isSuccess) {
                    button.clearAnimation()
                }
            }
        }

        search.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                quizViewModel.quizzes.value?.run {
                    val keywords = (newText ?: "").split(" ").map { it.lowercase() }.toList()
                    val query = filter { quiz -> keywords.all { quiz.name.lowercase().contains(it) } }

                    quizList.swapAdapter(QuizListAdapter(requireContext(), query), true)
                }

                return false
            }

            override fun onQueryTextSubmit(query: String?) = false
        })
    }
}