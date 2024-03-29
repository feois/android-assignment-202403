package com.wilson.assignment

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TabsAdapter(fragmentActivity: FragmentActivity, private val tabs: Array<Fragment>): FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount() = tabs.size
    override fun createFragment(position: Int) = tabs[position]
}

class UserViewModel: ViewModel() {
    private val CREDENTIAL = stringPreferencesKey("credential")

    private val userLiveData = MutableLiveData<User?>()
    val user: LiveData<User?> get() = userLiveData

    private val signUpResultLiveData = MutableLiveData<Result<Unit>>()
    val signUpResult: LiveData<Result<Unit>> get() = signUpResultLiveData

    private val loginResultLiveData = MutableLiveData<Result<Unit>>()
    val loginResult: LiveData<Result<Unit>> get() = loginResultLiveData

    private val databaseExceptionLiveData = MutableLiveData<Throwable>()
    val databaseException: LiveData<Throwable> get() = databaseExceptionLiveData

    fun readCredential(context: Context, successCallback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data.map { it[CREDENTIAL] }.first()?.let { username ->
                User.getUser(username)
                        .addOnSuccessListener { result ->
                            result.fold({
                                userLiveData.value = it
                                successCallback()
                            }, {
                                databaseExceptionLiveData.value = it
                            })
                        }
                        .addOnFailureListener { databaseExceptionLiveData.value = it }
            }
        }
    }

    private suspend fun storeCredential(context: Context) {
        withContext(Dispatchers.IO) {
            user.value?.run { context.dataStore.edit { it[CREDENTIAL] = username } }
        }
    }

    fun login(username: String, password: String, credentialContext: Context? = null) {
        User.collection.document(username).get()
                .addOnSuccessListener {
                    when {
                        !it.exists() -> loginResultLiveData.value = Result.failure(UserNotFoundException(username))
                        it.getString(User.PASSWORD_FIELD) != HashUtils.sha256(password)
                                -> loginResultLiveData.value = Result.failure(IncorrectPasswordException())
                        else -> it.data!!.toUser(username).fold({ user ->
                            userLiveData.value = user
                            credentialContext?.let { context ->
                                viewModelScope.launch {
                                    storeCredential(context)
                                }
                            }
                            loginResultLiveData.value = Result.success(Unit)
                        }, { e ->
                            databaseExceptionLiveData.value = e
                        })
                    }
                }
                .addOnFailureListener { databaseExceptionLiveData.value = it }
    }

    fun signUp(user: User, password: String, credentialContext: Context? = null) {
        User.hasUser(user.username)
                .addOnSuccessListener { userExists ->
                    when {
                        userExists -> signUpResultLiveData.value = Result.failure(UserAlreadyExistsException(user.username))
                        else -> User.collection.document(user.username)
                                .set(user.toMap(password))
                                .addOnSuccessListener {
                                    credentialContext?.let { context ->
                                        viewModelScope.launch {
                                            storeCredential(context)
                                        }
                                    }
                                    signUpResultLiveData.value = Result.success(Unit)
                                }
                                .addOnFailureListener { databaseExceptionLiveData.value = it }
                    }
                }
                .addOnFailureListener { databaseExceptionLiveData.value = it }
    }

    fun logout(credentialContext: Context? = null) {
        userLiveData.value = null
        credentialContext?.apply {
            viewModelScope.launch {
                dataStore.edit { it.remove(CREDENTIAL) }
            }
        }
    }
}

class MainActivity : AppCompatActivity(), LogInFragment.EventListener, SignUpFragment.EventListener {
    private val quizzesFragment = QuizzesFragment()
    private val notificationsFragment = NotificationsFragment()
    private val userProfileFragment = UserProfileFragment()
    private val logInFragment = LogInFragment()
    private val signUpFragment = SignUpFragment()

    private val userViewModel: UserViewModel by viewModels()

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

        val menu = findViewById<BottomNavigationView>(R.id.menu)

        accountFragment = logInFragment

        userViewModel.readCredential(this) {
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

        userViewModel.databaseException.observe(this) {
            longToast("Error: ${it::class.simpleName}")
            logError("Database Error encountered", it)
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