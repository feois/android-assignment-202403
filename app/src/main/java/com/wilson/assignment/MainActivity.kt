package com.wilson.assignment

import android.os.Bundle
import android.widget.SearchView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private val quizzesFragment: QuizzesFragment = QuizzesFragment()
    val fragments = arrayOf(quizzesFragment, NotificationsFragment(), UserProfileFragment())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initQuizzes(resources)

        val tabs = findViewById<ViewPager2>(R.id.tabs)
        val menu = findViewById<BottomNavigationView>(R.id.menu)
        val search = findViewById<SearchView>(R.id.search)

        tabs.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int) = fragments[position]
            override fun getItemCount() = 3
        }

        tabs.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                menu.menu.getItem(position).setChecked(true)
            }
        })

        menu.setOnItemSelectedListener {
            tabs.currentItem = when (it.itemId) {
                R.id.quizzes -> 0
                R.id.notifications -> 1
                R.id.user_profile -> 2
                else -> 0
            }
            true
        }

        search.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                val keywords = (newText ?: "").split(" ").map { it.lowercase() }.toList()
                val query = Quizzes.filter { quiz -> keywords.all { quiz.name.lowercase().contains(it) } }

                quizzesFragment.setQuizzes(query)

                return false
            }

            override fun onQueryTextSubmit(query: String?) = false
        })
    }
}