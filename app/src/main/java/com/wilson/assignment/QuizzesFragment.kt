package com.wilson.assignment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class QuizListAdapter(
    private val context: Context,
    private val quizList: List<Quiz>,
    private val lifecycleOwner: LifecycleOwner,
    private val userViewModel: UserViewModel,
): RecyclerView.Adapter<QuizListAdapter.QuizViewHolder>() {
    inner class QuizViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        var quiz: Quiz? = null; private set

        private val star = view.findViewById<ImageView>(R.id.quizStar)
        private val title = view.findViewById<TextView>(R.id.quizName)
        private val statusText = view.findViewById<TextView>(R.id.quizStatusText)
        private val status = view.findViewById<ProgressBar>(R.id.quizStatus)
        private val statusPercentage = view.findViewById<TextView>(R.id.quizStatusPercentage)

        fun setQuiz(q: Quiz, callback: () -> Unit) {
            quiz = q
            title.text = q.name
            view.setOnClickListener {
                callback()
            }
        }

        fun setStatus(marks: Int?) {
            val total = quiz?.totalMarks

            if (marks == null) {
                star.setImageResource(android.R.drawable.btn_star_big_off)
                statusText.text = "Uncompleted"
                status.visibility = View.INVISIBLE
                statusPercentage.visibility = View.INVISIBLE
            }
            else if (total != null) {
                star.setImageResource(android.R.drawable.btn_star_big_on)
                statusText.text = "Completed: $marks/${total}"

                context.markProgress(marks, total, status, statusPercentage)

                status.visibility = View.VISIBLE
                statusPercentage.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = QuizViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.quiz_item, parent, false))
            .apply {
                userViewModel.user.observe(lifecycleOwner) {
                    quiz?.run {
                        setStatus(it?.results?.get(id))
                    }
                }
            }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) = holder.run {
        val quiz = quizList[position]

        setQuiz(quiz) {
            userViewModel.startQuiz(context, quiz.id)
        }

        setStatus(userViewModel.user.value?.results?.get(quiz.id))
    }

    override fun getItemCount() = quizList.size
}

/**
 * A simple [Fragment] subclass.
 */
class QuizzesFragment : Fragment() {
    private val userViewModel: UserViewModel by activityViewModels()
    private val quizViewModel: QuizViewModel by activityViewModels()

    private lateinit var quizList: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_quizzes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val search = view.findViewById<SearchView>(R.id.search)
        val refresh = view.findViewById<ImageButton>(R.id.refreshQuizzes)
        val progress = view.findViewById<ProgressBar>(R.id.progressBar)

        if (quizViewModel.quizzes.value != null) {
            progress.visibility = View.GONE
        }

        quizList = view.findViewById(R.id.quizList)
        quizList.layoutManager = LinearLayoutManager(requireContext())

        quizViewModel.quizzes.observe(viewLifecycleOwner) {
            quizList.adapter = getAdapter(it)
            progress.visibility = View.GONE
        }

        view.findViewById<TextView>(R.id.warnLogin).visibility =
            if (userViewModel.user.value == null) { View.VISIBLE } else { View.GONE }

        refresh.setOnClickListener { button ->
            button.animation = RotateAnimation(0f, 360f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
                    .apply {
                        repeatCount = -1
                        duration = 1000
                    }

            progress.visibility = View.VISIBLE
            requireContext().updateQuizzes(quizViewModel).addOnCompleteListener {
                button.clearAnimation()
            }
        }

        search.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                quizViewModel.quizzes.value?.run {
                    val keywords = (newText ?: "").split(" ").map { it.lowercase() }.toList()
                    val query = filter { quiz -> keywords.all { quiz.name.lowercase().contains(it) } }

                    quizList.swapAdapter(getAdapter(query), true)
                }

                return false
            }

            override fun onQueryTextSubmit(query: String?) = false
        })
    }

    private fun getAdapter(quizzes: List<Quiz>) = QuizListAdapter(requireContext(), quizzes, viewLifecycleOwner, userViewModel)
}