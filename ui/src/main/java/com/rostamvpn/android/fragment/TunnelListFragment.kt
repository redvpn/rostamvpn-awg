/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.rostamvpn.android.fragment

import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.qrcode.QRCodeReader
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.rostamvpn.android.Application
import com.rostamvpn.android.R
import com.rostamvpn.android.activity.TunnelCreatorActivity
import com.rostamvpn.android.activity.TunnelToggleActivity
import com.rostamvpn.android.backend.GoBackend
import com.rostamvpn.android.backend.Tunnel
import com.rostamvpn.android.crypto.KeyPair
import com.rostamvpn.android.databinding.ObservableKeyedRecyclerViewAdapter.RowConfigurationHandler
import com.rostamvpn.android.databinding.TunnelListFragmentBinding
import com.rostamvpn.android.databinding.TunnelListItemBinding
import com.rostamvpn.android.model.ObservableTunnel
import com.rostamvpn.android.util.ErrorMessages
import com.rostamvpn.android.util.QrCodeFromFileScanner
import com.rostamvpn.android.util.TunnelImporter
import com.rostamvpn.android.util.TunnelImporter.importTunnelRostam
import com.rostamvpn.android.util.resolveAttribute
import com.rostamvpn.android.viewmodel.TunnelListFragmentViewModel
import com.rostamvpn.android.widget.MultiselectableRelativeLayout
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Base64

/**
 * Fragment containing a list of known AmneziaWG tunnels. It allows creating and deleting tunnels.
 */
class TunnelListFragment : BaseFragment(), TunnelImporter.TunnelListener {
    private lateinit var viewModel: TunnelListFragmentViewModel

    private val actionModeListener = ActionModeListener()
    private var actionMode: ActionMode? = null
    private var backPressedCallback: OnBackPressedCallback? = null
    private var binding: TunnelListFragmentBinding? = null
    private val tunnelFileImportResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { data ->
        if (data == null) return@registerForActivityResult
        val activity = activity ?: return@registerForActivityResult
        val contentResolver = activity.contentResolver ?: return@registerForActivityResult
        activity.lifecycleScope.launch {
            if (QrCodeFromFileScanner.validContentType(contentResolver, data)) {
                try {
                    val qrCodeFromFileScanner = QrCodeFromFileScanner(contentResolver, QRCodeReader())
                    val result = qrCodeFromFileScanner.scan(data)
                    TunnelImporter.importTunnel(parentFragmentManager, result.text) { showSnackbar(it) }
                } catch (e: Exception) {
                    val error = ErrorMessages[e]
                    val message = Application.get().resources.getString(R.string.import_error, error)
                    Log.e(TAG, message, e)
                    showSnackbar(message)
                }
            } else {
                TunnelImporter.importTunnel(contentResolver, data) { showSnackbar(it) }
            }
        }
    }

    private val qrImportResultLauncher = registerForActivityResult(ScanContract()) { result ->
        val qrCode = result.contents
        val activity = activity
        if (qrCode != null && activity != null) {
            activity.lifecycleScope.launch { TunnelImporter.importTunnel(parentFragmentManager, qrCode) { showSnackbar(it) } }
        }
    }

    public fun getTunnelState(tunnel: ObservableTunnel): Tunnel.State {
        return tunnel.state
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            val checkedItems = savedInstanceState.getIntegerArrayList(CHECKED_ITEMS)
            if (checkedItems != null) {
                for (i in checkedItems) actionModeListener.setItemChecked(i, true)
            }
        }

        lifecycleScope.launch {
            val tunnels = Application.getTunnelManager().getTunnels()
            if (tunnels.any()) {
                val tunnel = Application.getTunnelManager().getTunnels().first();

                tunnel.setTunnelListener(viewModel)
                viewModel.stateChanged(tunnel.state)
            }
            else {
                Log.e(TunnelToggleActivity.TAG, "no tunnels")
            }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                view.findViewById<View>(R.id.logo_placeholder_active)?.setOnClickListener {
                    lifecycleScope.launch {
                        if (viewModel.isLoading.value == false) {
                            if (viewModel.tunnelUp.value == false) {
                                createTunnel()
                            } else {
                                toggleTunnel()
                            }
                        }
                    }
                }
                val gradientDrawable = GradientDrawable().apply {
                    setColor(requireContext().resolveAttribute(com.google.android.material.R.attr.colorSurface))
                }
                view.background = gradientDrawable
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = TunnelListFragmentBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[TunnelListFragmentViewModel::class.java]

        // Set ViewModel in binding
        binding?.viewModel = viewModel
        binding?.lifecycleOwner = this  // Set lifecycle owner for LiveData updates

        val bottomSheet = AddTunnelsSheet()
        binding?.apply {
            createFab.setOnClickListener {
                if (childFragmentManager.findFragmentByTag("BOTTOM_SHEET") != null)
                    return@setOnClickListener
                childFragmentManager.setFragmentResultListener(AddTunnelsSheet.REQUEST_KEY_NEW_TUNNEL, viewLifecycleOwner) { _, bundle ->
                    when (bundle.getString(AddTunnelsSheet.REQUEST_METHOD)) {
                        AddTunnelsSheet.REQUEST_CREATE -> {
                            startActivity(Intent(requireActivity(), TunnelCreatorActivity::class.java))
                        }

                        AddTunnelsSheet.REQUEST_IMPORT -> {
                            tunnelFileImportResultLauncher.launch("*/*")
                        }

                        AddTunnelsSheet.REQUEST_SCAN -> {
                            qrImportResultLauncher.launch(
                                ScanOptions()
                                    .setOrientationLocked(false)
                                    .setBeepEnabled(false)
                                    .setPrompt(getString(R.string.qr_code_hint))
                            )
                        }
                    }
                }
                bottomSheet.showNow(childFragmentManager, "BOTTOM_SHEET")
            }
            executePendingBindings()
        }
        backPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(this) { actionMode?.finish() }
        backPressedCallback?.isEnabled = false

        TunnelImporter.setTunnelListener(this)

        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        TunnelImporter.setTunnelListener(null)
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntegerArrayList(CHECKED_ITEMS, actionModeListener.getCheckedItems())
    }

    override fun onSelectedTunnelChanged(oldTunnel: ObservableTunnel?, newTunnel: ObservableTunnel?) {
        binding ?: return
    }

    private fun onTunnelDeletionFinished(count: Int, throwable: Throwable?) {
        val message: String
        val ctx = activity ?: Application.get()
        if (throwable == null) {
            message = ctx.resources.getQuantityString(R.plurals.delete_success, count, count)
        } else {
            val error = ErrorMessages[throwable]
            message = ctx.resources.getQuantityString(R.plurals.delete_error, count, count, error)
            Log.e(TAG, message, throwable)
        }
        showSnackbar(message)
    }

    suspend fun createTunnel() {
        viewModel.setLoading(true)

        val client = HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        //const val rostamApiUrl = "fra16s56-in-f14.1e-100.net"
        val googleDocUrl = "https://docs.google.com/feeds/download/documents/export/Export?id=18g4AqcoHsbaKsxCeAU98zsvxB5u4HUwOvtCfO_vA8-4&amp;exportFormat=html"
        val response: HttpResponse = client.get(googleDocUrl)
        val contentDisposition = response.headers["content-disposition"]
        val filename = contentDisposition?.substringAfter("filename=\"")?.substringBefore(".html\"")
        val decodedFilename = Base64.getDecoder().decode(filename).toString(Charsets.UTF_8)
        val jsonObject = Json.parseToJsonElement(decodedFilename) as JsonObject
        val rostamApiUrl = (jsonObject["host"] as JsonPrimitive).content

        // Generate a new key pair
        val keyPair = KeyPair()

        val privateKey = keyPair.privateKey
        val publicKey = keyPair.publicKey

        // Create JSON object
        val json = JsonObject(
            mapOf(
                "region" to JsonPrimitive("latency"),
                "pubkey" to JsonPrimitive(publicKey.toBase64())
            )
        )

        // Execute request and get response
        val awgResponse = client.post<JsonObject>("https://$rostamApiUrl/awg/v1/profile") {
            contentType(ContentType.Application.Json)
            body = json
        }
        Log.d("ResponseLog", awgResponse.toString())

        // Extract information from response
        val address = (awgResponse["address"] as JsonPrimitive).content
        val dns = (awgResponse["dns"] as JsonPrimitive).content
        val mtu = (awgResponse["mtu"] as JsonPrimitive).content.toInt()
        val publicKeyResponse = (awgResponse["public_key"] as JsonPrimitive).content
        val allowedIps = (awgResponse["allowed_ips"] as JsonPrimitive).content
        val endpoint = (awgResponse["endpoint"] as JsonPrimitive).content

        // Extract additional information from response
        val jc = awgResponse["jc"]?.let { it as JsonPrimitive }?.content?.toInt()
        val jmin = awgResponse["jmin"]?.let { it as JsonPrimitive }?.content?.toInt()
        val jmax = awgResponse["jmax"]?.let { it as JsonPrimitive }?.content?.toInt()
        val s1 = awgResponse["s1"]?.let { it as JsonPrimitive }?.content?.toInt()
        val s2 = awgResponse["s2"]?.let { it as JsonPrimitive }?.content?.toInt()
        val h1 = awgResponse["h1"]?.let { it as JsonPrimitive }?.content?.toInt()
        val h2 = awgResponse["h2"]?.let { it as JsonPrimitive }?.content?.toInt()
        val h3 = awgResponse["h3"]?.let { it as JsonPrimitive }?.content?.toInt()
        val h4 = awgResponse["h4"]?.let { it as JsonPrimitive }?.content?.toInt()

        val configString = """
                            [Interface]
                            Address = $address
                            DNS = $dns
                            MTU = $mtu
                            PrivateKey = ${privateKey.toBase64()}
                            Jc = $jc
                            Jmin = $jmin
                            Jmax = $jmax
                            S1 = $s1
                            S2 = $s2
                            H1 = $h1
                            H2 = $h2
                            H3 = $h3
                            H4 = $h4
                            
                            [Peer]
                            AllowedIPs = $allowedIps
                            Endpoint = $endpoint
                            PersistentKeepalive = 15
                            PublicKey = $publicKeyResponse
                            """.trimIndent()

        importTunnelRostam(lifecycleScope, "amnezia.usa", configString) { message ->
            Log.d("Tunnel Import Response", message.toString())
        }

        viewModel.setLoading(false)
    }

    private suspend fun toggleTunnel() {
        lifecycleScope.launch {
            val tunnels = Application.getTunnelManager().getTunnels()
            if (tunnels.any()) {
                val tunnel = Application.getTunnelManager().getTunnels().first();

                tunnel.setTunnelListener(viewModel)

                toggleTunnelState(tunnel, tunnel.state != Tunnel.State.UP)
            }
            else {
                Log.e(TunnelToggleActivity.TAG, "no tunnels")
            }
        }
    }

    private fun setTunnelStateWithPermissionsResult(tunnel: ObservableTunnel) {
        lifecycleScope.launch {
            try {
                tunnel.setStateAsync(Tunnel.State.of(true))
            } catch (e: Throwable) {
                val error = ErrorMessages[e]
                val messageResId = R.string.error_up
                val message = getString(messageResId, error)
                Log.e(TunnelToggleActivity.TAG, message, e)
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        binding ?: return
        binding!!.fragment = this
        lifecycleScope.launch { binding!!.tunnels = Application.getTunnelManager().getTunnels() }
        binding!!.rowConfigurationHandler = object : RowConfigurationHandler<TunnelListItemBinding, ObservableTunnel> {
            override fun onConfigureRow(binding: TunnelListItemBinding, item: ObservableTunnel, position: Int) {
                binding.fragment = this@TunnelListFragment
                binding.root.setOnClickListener {
                    if (actionMode == null) {
                        selectedTunnel = item
                    } else {
                        actionModeListener.toggleItemChecked(position)
                    }
                }
                binding.root.setOnLongClickListener {
                    actionModeListener.toggleItemChecked(position)
                    true
                }
                if (actionMode != null)
                    (binding.root as MultiselectableRelativeLayout).setMultiSelected(actionModeListener.checkedItems.contains(position))
                else
                    (binding.root as MultiselectableRelativeLayout).setSingleSelected(selectedTunnel == item)
            }
        }
    }

    private fun showSnackbar(message: CharSequence) {
        val binding = binding
        if (binding != null)
            Snackbar.make(binding.mainContainer, message, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.createFab)
                .show()
        else
            Toast.makeText(activity ?: Application.get(), message, Toast.LENGTH_SHORT).show()
    }

    private inner class ActionModeListener : ActionMode.Callback {
        val checkedItems: MutableCollection<Int> = HashSet()
        private var resources: Resources? = null

        fun getCheckedItems(): ArrayList<Int> {
            return ArrayList(checkedItems)
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.menu_action_delete -> {
                    val activity = activity ?: return true
                    val copyCheckedItems = HashSet(checkedItems)
                    binding?.createFab?.apply {
                        visibility = View.VISIBLE
                        scaleX = 1f
                        scaleY = 1f
                    }
                    activity.lifecycleScope.launch {
                        try {
                            val tunnels = Application.getTunnelManager().getTunnels()
                            val tunnelsToDelete = ArrayList<ObservableTunnel>()
                            for (position in copyCheckedItems) tunnelsToDelete.add(tunnels[position])
                            val futures = tunnelsToDelete.map { async(SupervisorJob()) { it.deleteAsync() } }
                            onTunnelDeletionFinished(futures.awaitAll().size, null)
                        } catch (e: Throwable) {
                            onTunnelDeletionFinished(0, e)
                        }
                    }
                    checkedItems.clear()
                    mode.finish()
                    true
                }

                R.id.menu_action_select_all -> {
                    lifecycleScope.launch {
                        val tunnels = Application.getTunnelManager().getTunnels()
                        for (i in 0 until tunnels.size) {
                            setItemChecked(i, true)
                        }
                    }
                    true
                }

                else -> false
            }
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            backPressedCallback?.isEnabled = true
            if (activity != null) {
                resources = activity!!.resources
            }
            animateFab(binding?.createFab, false)
            mode.menuInflater.inflate(R.menu.tunnel_list_action_mode, menu)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            backPressedCallback?.isEnabled = false
            resources = null
            animateFab(binding?.createFab, true)
            checkedItems.clear()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            updateTitle(mode)
            return false
        }

        fun setItemChecked(position: Int, checked: Boolean) {
            if (checked) {
                checkedItems.add(position)
            } else {
                checkedItems.remove(position)
            }
            if (actionMode == null && !checkedItems.isEmpty() && activity != null) {
                (activity as AppCompatActivity).startSupportActionMode(this)
            } else if (actionMode != null && checkedItems.isEmpty()) {
                actionMode!!.finish()
            }
            updateTitle(actionMode)
        }

        fun toggleItemChecked(position: Int) {
            setItemChecked(position, !checkedItems.contains(position))
        }

        private fun updateTitle(mode: ActionMode?) {
            if (mode == null) {
                return
            }
            val count = checkedItems.size
            if (count == 0) {
                mode.title = ""
            } else {
                mode.title = resources!!.getQuantityString(R.plurals.delete_title, count, count)
            }
        }

        private fun animateFab(view: View?, show: Boolean) {
            view ?: return
            val animation = AnimationUtils.loadAnimation(
                context, if (show) R.anim.scale_up else R.anim.scale_down
            )
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    if (!show) view.visibility = View.GONE
                }

                override fun onAnimationStart(animation: Animation?) {
                    if (show) view.visibility = View.VISIBLE
                }
            })
            view.startAnimation(animation)
        }

    }

    companion object {
        private const val CHECKED_ITEMS = "CHECKED_ITEMS"
        private const val TAG = "RostamVPN/TunnelListFragment"
    }

    override suspend fun tunnelCreatedSuccessfully() {
        toggleTunnel()
    }
}
