package com.wilson.assignment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.toObject

data class Item(val username: String = "", val quizId: String = "", val time: Timestamp = Timestamp(0, 0))

class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {
    val contentText = view.findViewById<TextView>(R.id.textView6)
    val timeText = view.findViewById<TextView>(R.id.textView7)
    val template = contentText.text.toString()

    var user: User? = null
    var quiz: Quiz? = null

    fun initContent(user: User, quiz: Quiz) {
        val marks = user.results[quiz.id]!!
        val total = quiz.totalMarks
        val perct = marks * 100 / total

        contentText.text = template
                .replace("{user}", user.fullName)
                .replace("{quiz}", quiz.name)
                .replace("{marks}", "$marks/$total ($perct%)")
    }
}

class BlankFragment : Fragment() {
    val quizViewModel: QuizViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.fragment_blank, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        val col = db.collection("items")

        recycler.layoutManager = LinearLayoutManager(requireContext())

//        col.get().addOnSuccessListener {
//            recycler.adapter = getAdapter(it.documents.map { doc -> doc.toObject<Item>()!! })
//        }

        col.addSnapshotListener { value, _ ->
            recycler.swapAdapter(getAdapter(value!!.documents.map { it.toObject<Item>()!! }), true)
        }
    }

    fun getAdapter(list: List<Item>) = object: RecyclerView.Adapter<ViewHolder>() {
        override fun getItemCount() = list.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
        = ViewHolder(LayoutInflater.from(requireContext()).inflate(R.layout.item, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            User.getUser(list[position].username).addOnSuccessListener {
                it.getOrNull()?.run {
                    holder.initContent(this, quizViewModel.quizzesMap[list[position].quizId]!!)
                }
            }

            holder.timeText.text = formatTime(list[position].time)
        }
    }
}