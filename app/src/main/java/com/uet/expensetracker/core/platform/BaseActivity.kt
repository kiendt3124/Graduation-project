package com.uet.expensetracker.core.platform

import androidx.activity.ComponentActivity


/**
 * Base Activity class with helper methods for handling fragment transactions and back button
 * events.
 *
 * @see AppCompatActivity
 */
abstract class BaseActivity : ComponentActivity() {

//    private lateinit var binding: ActivityLayoutBinding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityLayoutBinding.inflate(layoutInflater)
//
//        setContentView(binding.root)
//        setSupportActionBar(binding.toolBarContainer.toolbar)
//        addFragment(savedInstanceState)
//    }
//
//    @Deprecated("Deprecated in Java")
//    override fun onBackPressed() {
//        (supportFragmentManager.findFragmentById(binding.fragmentContainer.id) as BaseFragment).onBackPressed()
//        super.onBackPressed()
//    }
//
//    fun toolbar() = binding.toolBarContainer.toolbar
//
//    fun fragmentContainer() = binding.fragmentContainer
//    fun progressBar() = binding.toolBarContainer.progress
//
//    private fun addFragment(savedInstanceState: Bundle?) =
//        savedInstanceState ?: supportFragmentManager.inTransaction {
//            add(binding.fragmentContainer.id, fragment())
//        }

//    abstract fun fragment(): BaseFragment
}