/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Menu
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.fragment.NavHostFragment
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.jiaoay.jrime.BuildConfig
import com.jiaoay.jrime.R
import com.jiaoay.jrime.core.data.DataManager
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.soundeffect.SoundEffectManager
import com.jiaoay.jrime.databinding.ActivityMainBinding
import com.jiaoay.jrime.ime.core.TrimeInputMethodService
import com.jiaoay.jrime.util.item
import com.jiaoay.jrime.util.parcelable
import com.jiaoay.jrime.util.startActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.views.topPadding
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val uiMode by AppPrefs.defaultInstance().advanced.uiMode

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var testInputPanel: TestInputPanel? = null
    private var imeServiceConnection: ServiceConnection? = null

    /**
     * Pre-warm the in-process IME service for this activity's lifetime.
     *
     * The IME runs in the main process, so requesting the IME from this
     * activity while the service is cold deadlocks: the window-organizer
     * IME-show transaction blocks this process's main thread waiting for
     * system_server, while the service's own creation binder transaction
     * queues behind that same blocked thread -- after 5s the activity is
     * force-finished ("top resumed state loss timeout"). The Test input
     * panel and any other editor here request the IME, so creating the
     * service ahead of any show request removes the bind step from that
     * critical path. Binding one's own component needs no permission.
     */
    private fun prewarmImeService() {
        if (imeServiceConnection != null) return
        val connection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {}

                override fun onServiceDisconnected(name: ComponentName) {}
            }
        runCatching {
            bindService(Intent(this, TrimeInputMethodService::class.java), connection, 0)
        }.onSuccess {
            imeServiceConnection = connection
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val uiMode =
            when (uiMode) {
                AppPrefs.Advanced.UiMode.AUTO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                AppPrefs.Advanced.UiMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                AppPrefs.Advanced.UiMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
        AppCompatDelegate.setDefaultNightMode(uiMode)
        super.onCreate(savedInstanceState)
        prewarmImeService()
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left
                rightMargin = systemBars.right
                bottomMargin = maxOf(systemBars.bottom, ime.bottom)
            }
            binding.mainToolbar.root.topPadding = systemBars.top
            windowInsets
        }
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.root,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: List<WindowInsetsAnimationCompat?>,
                ): WindowInsetsCompat {
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                    binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = maxOf(systemBars.bottom, ime.bottom)
                    }
                    return insets
                }
            },
        )
        WindowCompat
            .getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false

        setContentView(binding.root)
        // always show toolbar back arrow icon
        binding.mainToolbar.toolbar.navigationIcon =
            DrawerArrowDrawable(this).apply {
                progress = 1f
                color = ContextCompat.getColor(this@MainActivity, R.color.toolbarForegroundColor)
            }
        // show menu icon and other action icons on toolbar
        // don't use `setSupportActionBar(binding.toolbar)` here,
        // because navController would change toolbar title, we need to control it by ourselves
        setupToolbarMenu(binding.mainToolbar.toolbar.menu)
        navController = binding.navHostFragment.getFragment<NavHostFragment>().navController
        navController.graph = NavigationRoute.createGraph(navController)
        binding.mainToolbar.toolbar.setNavigationOnClickListener {
            // prevent navigate up when child fragment has enabled `OnBackPressedCallback`
            if (onBackPressedDispatcher.hasEnabledCallbacks()) {
                onBackPressedDispatcher.onBackPressed()
                return@setNavigationOnClickListener
            }
            // "minimize" the activity if we can't go back
            navController.navigateUp() || onSupportNavigateUp() || moveTaskToBack(false)
        }
        onBackPressedDispatcher.addCallback {
            if (binding.testInputPanel.isVisible) {
                binding.testInputPanel.dismiss()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
        testInputPanel = binding.testInputPanel
        viewModel.toolbarTitle.observe(this) {
            binding.mainToolbar.toolbar.title = it
        }
        navController.addOnDestinationChangedListener { _, dest, _ ->
            dest.label?.let { viewModel.setToolbarTitle(it.toString()) }
            binding.mainToolbar.toolbar.subtitle =
                if (dest.hasRoute<NavigationRoute.Main>()) {
                    getString(R.string.trime_app_slogan)
                } else {
                    ""
                }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        processIntent(intent)
        checkNotificationPermission()
        binding.setupBanner.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        binding.deployBanner.setOnClickListener { runRepairDeploy() }
        // The banner depends on the released staging tree, which the engine
        // start populates; refresh it once the first start has settled.
        lifecycleScope.launch {
            viewModel.rime.runOnReady { }
            withContext(Dispatchers.Main.immediate) {
                if (!isFinishing) {
                    firstStartSettled = true
                    updateDeployBanner()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_RUN -> {
                val route = intent.parcelable<NavigationRoute>(EXTRA_SETTINGS_ROUTE) ?: return
                navController.popBackStack(NavigationRoute.Main, false)
                navController.navigate(route)
            }
        }
    }

    private fun setupToolbarMenu(menu: Menu) {
        val optionMenuItems = listOf(
            menu.item(R.string.deploy, R.drawable.ic_baseline_refresh_reversed_24, showAsAction = true) {
                runRepairDeploy()
            },
            menu.item(R.string.test_input, R.drawable.ic_baseline_keyboard_24, showAsAction = true) {
                testInputPanel?.show(window)
            },
            menu.item(R.string.developer) {
                navController.navigate(NavigationRoute.Developer)
            },
            menu.item(R.string.about) {
                navController.navigate(NavigationRoute.About)
            },
        )
        optionMenuItems.forEach { item ->
            viewModel.topOptionsMenu.observe(this) { enabled ->
                item.isVisible = enabled
            }
        }
        menu.item(R.string.edit, R.drawable.ic_baseline_edit_24, showAsAction = true) {
            viewModel.toolbarEditButtonOnClickListener.value?.invoke()
        }.apply {
            viewModel.toolbarEditButtonVisible.observe(this@MainActivity) {
                isVisible = it
            }
        }
        menu.item(R.string.delete, R.drawable.ic_baseline_delete_24, showAsAction = true) {
            viewModel.toolbarDeleteButtonOnClickListener.value?.invoke()
        }.apply {
            viewModel.toolbarDeleteButtonOnClickListener.observe(this@MainActivity) {
                isVisible = it != null
            }
        }
        menu.forEach { item ->
            // show menu item on demand
            item.isVisible = false
        }
    }

    override fun onResume() {
        super.onResume()
        SoundEffectManager.init()
        updateSetupBanner()
        showSetupDialogIfNeeded()
        updateDeployBanner()
    }

    /**
     * The wizard is gone; enabling and selecting the IME are system-forced steps
     * the app cannot perform. The banner carries them as a passive hint; the
     * dialog below makes the first run impossible to miss.
     */
    private fun imeSetupState(): Pair<Boolean, Boolean> {
        val imm = getSystemService(InputMethodManager::class.java)
        val enabled = imm?.enabledInputMethodList?.any { it.packageName == packageName } == true
        // Compare the settings id's package part exactly: a leftover id of a
        // sibling build (the .debug suffix) starts with the same package name
        // and would pass a startsWith check while the real IME is unselected.
        val selected = Settings.Secure
            .getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/') == packageName
        return enabled to selected
    }

    private fun updateSetupBanner() {
        val (enabled, selected) = imeSetupState()
        binding.setupBanner.isVisible = !(enabled && selected)
    }

    /** Session-level "later" latch: one dismissal stops the dialog until the process dies. */
    private var setupDialogDismissed = false

    /**
     * The explicit first-run guide: a fresh install that was never enabled
     * leaves typing silently dead, and the banner is too easy to miss. The
     * dialog names the exact missing step and jumps to the system page --
     * coming back from settings re-runs the check, so the guide closes
     * itself the moment the user finishes the step.
     */
    private fun showSetupDialogIfNeeded() {
        if (setupDialogDismissed || isFinishing) return
        val (enabled, selected) = imeSetupState()
        if (enabled && selected) return
        val (title, message) =
            if (!enabled) {
                R.string.setup_dialog_enable_title to R.string.setup_dialog_enable_message
            } else {
                R.string.setup_dialog_select_title to R.string.setup_dialog_select_message
            }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.setup_dialog_go) { _, _ ->
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
            .setNegativeButton(R.string.setup_dialog_later) { _, _ -> setupDialogDismissed = true }
            .setOnCancelListener { setupDialogDismissed = true }
            .show()
    }

    /** Set once the first engine start has settled, i.e. the released staging
     * tree the deploy banner judges is actually there to judge. */
    private var firstStartSettled = false

    /**
     * The non-blocking repair banner: startup no longer deploys, so a fresh
     * install whose prebuilt artifacts never made it into the released tree
     * must not soft-lock — the engine starts, typing stays dead, and the
     * banner points the user at the deploy menu repair path.
     *
     * The banner depends on the released staging tree, which the engine start
     * populates. onResume can run before the first start settles, and a fresh
     * install's still-empty user dir would flash the banner until the ready
     * callback hides it again — so leave the banner (hidden in the layout)
     * alone until then; the runOnReady refresh in onCreate and the deploy
     * callbacks cover everything after that.
     */
    private fun updateDeployBanner() {
        if (!firstStartSettled) return
        binding.deployBanner.isVisible = !DataManager.hasStagingArtifacts()
    }

    /**
     * The repair path shared by the toolbar deploy menu and the banner:
     * recompile everything stale or missing, then refresh the banner from
     * what the deployment actually left behind.
     */
    private fun runRepairDeploy() {
        lifecycleScope.launch {
            viewModel.rime.runOnReady {
                runCatching { deploy() }
                    .onFailure { Timber.e(it, "Repair deployment failed") }
            }
            withContext(Dispatchers.Main.immediate) {
                if (!isFinishing) updateDeployBanner()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        testInputPanel?.dismiss()
    }

    override fun onDestroy() {
        imeServiceConnection?.let { runCatching { unbindService(it) } }
        imeServiceConnection = null
        testInputPanel = null
        super.onDestroy()
    }

    private fun checkNotificationPermission() {
        val permission = PermissionLists.getPostNotificationsPermission()
        if (XXPermissions.isGrantedPermission(this, permission)) {
            return
        } else {
            AlertDialog
                .Builder(this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.notification_permission_title)
                .setMessage(R.string.notification_permission_message)
                .setPositiveButton(R.string.grant_permission) { _, _ ->
                    XXPermissions
                        .with(this)
                        .permission(permission)
                        .request(null)
                }.setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    companion object {
        const val EXTRA_SETTINGS_ROUTE = "${BuildConfig.APPLICATION_ID}.EXTRA_SETTINGS_ROUTE"
    }
}
