package com.wilson.assignment

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class TabsAdapter(fragmentActivity: FragmentActivity, private val tabs: Array<Fragment>): FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount() = tabs.size
    override fun createFragment(position: Int) = tabs[position]
}

class MainActivity : AppCompatActivity(), LogInFragment.EventListener, SignUpFragment.EventListener {
    private val quizzesFragment = QuizzesFragment()
    private val notificationsFragment = NotificationsFragment()
    private val userProfileFragment = UserProfileFragment()
    private val logInFragment = LogInFragment()
    private val signUpFragment = SignUpFragment()

    private val userViewModel: UserViewModel by viewModels()
    private val quizViewModel: QuizViewModel by viewModels()

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

        quizViewModel.updateQuizzes()
                .addOnSuccessListener {
                    it.exceptionOrNull()?.run { errorToast("Failed to update quiz", this) }
                }
                .addOnFailureListener { errorToast("Failed to update quiz", it) }

        val menu = findViewById<BottomNavigationView>(R.id.menu)

        accountFragment = logInFragment

        userViewModel.readCredential(dataStore) {
            accountFragment = userProfileFragment
            refreshTabs()
        }

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

        userViewModel.user.observe(this) {
            if (it == null) {
                accountFragment = logInFragment
                refreshTabs()
            }
        }

        userViewModel.databaseException.observe(this) { logError("Database Error", it) }

        userViewModel.loginResult.observe(this) {
            if (it.isSuccess) {
                logInFragment.clearInput()
                signUpFragment.clearInput()
                accountFragment = userProfileFragment
                refreshTabs()
                shortToast("Successfully logged in!")
            }
        }

        userViewModel.signUpResult.observe(this) {
            if (it.isSuccess) {
                logInFragment.clearInput()
                signUpFragment.clearInput()
                accountFragment = userProfileFragment
                refreshTabs()
                shortToast("Successfully created account!")
            }
        }

        userViewModel.databaseException.observe(this) { errorToast("Database Error encountered", it) }

        onBackPressedDispatcher.addCallback(this) {
            AlertDialog.Builder(this@MainActivity)
                    .setTitle("Exit")
                    .setMessage("Do you want to exit the application")
                    .setPositiveButton("Yes") { _, _ ->
                        finish()
                    }
                    .setNegativeButton("No") { _, _ ->

                    }
                    .show()
        }
    }

    override fun onGoToSignUp() {
        accountFragment = signUpFragment
        refreshTabs()
    }

    override fun onGoToLogIn() {
        accountFragment = logInFragment
        refreshTabs()
    }

    private fun refreshTabs() {
        val item = tabs.currentItem

        tabs.adapter = TabsAdapter(this, fragments)
        tabs.setCurrentItem(item, false)
    }
}