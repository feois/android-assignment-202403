package com.wilson.assignment

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TabsAdapter(fragmentActivity: FragmentActivity, private val tabs: Array<Fragment>): FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount() = tabs.size
    override fun createFragment(position: Int) = tabs[position]
}

class MainActivity : AppCompatActivity(), UserProfileFragment.EventListener, LogInFragment.EventListener, SignUpFragment.EventListener {
    private val quizzesFragment = QuizzesFragment()
    private val notificationsFragment = NotificationsFragment()
    private val userProfileFragment = UserProfileFragment()
    private val logInFragment = LogInFragment()
    private val signUpFragment = SignUpFragment()

    private lateinit var accountFragment: Fragment

    private val fragments get() = arrayOf(quizzesFragment, notificationsFragment, accountFragment)

    private lateinit var tabs: ViewPager2

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

        val menu = findViewById<BottomNavigationView>(R.id.menu)

        accountFragment = logInFragment

        tabs = findViewById(R.id.tabs)
        tabs.adapter = TabsAdapter(this, fragments)
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
    }

    override fun onSignUp(user: User) {
        User.addUser(user).addOnSuccessListener {
            if (it == null) {
                Toast.makeText(this, "Unknown error", Toast.LENGTH_SHORT).show()
            }
            else if (it) {
                logInFragment.clearInput()
                signUpFragment.clearInput()
                accountFragment = userProfileFragment
                refreshTabs()
                Toast.makeText(this, "Successfully created account!", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Username ${user.username} has already been taken", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onGoToSignUp() {
        accountFragment = signUpFragment
        refreshTabs()
    }

    override fun onLogIn(username: String, password: String, newAccount: Boolean) {
        User.getUser(username)
                .addOnSuccessListener {
                    if (it == null) {
                        Toast.makeText(this, "User $username not found", Toast.LENGTH_LONG).show()
                    }
                    else if (it.password != password) {
                        Toast.makeText(this, "Password incorrect", Toast.LENGTH_LONG).show()
                    }
                    else {
                        logInFragment.clearInput()
                        signUpFragment.clearInput()
                        accountFragment = userProfileFragment
                        refreshTabs()
                        Toast.makeText(this, "Successfully logged in!", Toast.LENGTH_LONG).show()
                    }
                }
    }

    override fun onGoToLogIn() {
        accountFragment = logInFragment
        refreshTabs()
    }

    override fun onLogOut() {
        accountFragment = logInFragment
        refreshTabs()
    }

    private fun refreshTabs() {
        val item = tabs.currentItem

        tabs.adapter = TabsAdapter(this, fragments)
        tabs.setCurrentItem(item, false)
    }
}