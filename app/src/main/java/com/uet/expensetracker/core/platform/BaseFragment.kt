package com.uet.expensetracker.core.platform

import androidx.fragment.app.Fragment

/**
 * Base Fragment class with helper methods for handling views and back button events.
 *
 * @see Fragment
 */
abstract class BaseFragment : Fragment() {
    //    open fun onBackPressed() {}
//
//    internal fun firstTimeCreated(savedInstanceState: Bundle?) = savedInstanceState == null
//
//    internal fun showProgress() = progressStatus(View.VISIBLE)
//
//    internal fun hideProgress() = progressStatus(View.GONE)
//
//    private fun progressStatus(viewStatus: Int) =
//        with(activity) { if (this is BaseActivity) this.progressBar().visibility = viewStatus }
//
//    internal fun notify(@StringRes message: Int) =
//        Snackbar.make(viewContainer, message, Snackbar.LENGTH_SHORT).show()
//
//    internal fun notifyWithAction(
//        @StringRes message: Int,
//        @StringRes actionText: Int,
//        action: () -> Any
//    ) {
//        val snackBar = Snackbar.make(viewContainer, message, Snackbar.LENGTH_INDEFINITE)
//        snackBar.setAction(actionText) { _ -> action.invoke() }
//        snackBar.setActionTextColor(ContextCompat.getColor(appContext, color.colorTextPrimary))
//        snackBar.show()
//    }
}