/*
 * Copyright (c) 2023 Cyb3rKo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyb3rko.pincredible.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cyb3rko.pincredible.BuildConfig
import com.cyb3rko.pincredible.R
import com.cyb3rko.pincredible.crypto.CryptoManager
import com.cyb3rko.pincredible.crypto.CryptoManager.EnDecryptionException
import com.cyb3rko.pincredible.databinding.FragmentHomeBinding
import com.cyb3rko.pincredible.modals.ErrorDialog
import com.cyb3rko.pincredible.recycler.PinAdapter
import com.cyb3rko.pincredible.utils.ObjectSerializer
import com.cyb3rko.pincredible.utils.Vibration
import com.cyb3rko.pincredible.utils.openUrl
import com.cyb3rko.pincredible.utils.showDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var myContext: Context
    private val vibrator by lazy { Vibration.getVibrator(myContext) }
    private lateinit var adapter: PinAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        myContext = requireContext()
        addMenuProvider()

        adapter = PinAdapter {
            Vibration.vibrateClick(vibrator)
            findNavController().navigate(HomeFragmentDirections.homeToPinviewer(it))
        }
        binding.recycler.layoutManager = LinearLayoutManager(myContext)
        binding.recycler.adapter = adapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            val pins: List<String>
            try {
                pins = retrievePins()
                withContext(Dispatchers.Main) {
                    showSavedPins(pins)
                }
            } catch (e: EnDecryptionException) {
                Log.d("CryptoManager", e.customStacktrace)
                withContext(Dispatchers.Main) {
                    binding.progressBar.hide()
                    ErrorDialog.show(myContext, e)
                }
            }
        }

        binding.fab.setOnClickListener {
            Vibration.vibrateDoubleClick(vibrator)
            findNavController().navigate(HomeFragmentDirections.homeToPincreator())
        }
    }

    @Throws(EnDecryptionException::class)
    private fun retrievePins(): List<String> {
        val pinsFile = File(myContext.filesDir, "pins")
        return if (pinsFile.exists()) {
            @Suppress("UNCHECKED_CAST")
            (ObjectSerializer.deserialize(CryptoManager.decrypt(pinsFile)) as Set<String>).toList()
        } else {
            listOf()
        }
    }

    private fun showSavedPins(pins: List<String>) {
        binding.progressBar.hide()
        if (pins.isNotEmpty()) {
            adapter.submitList(pins.sorted())
            binding.chip.apply {
                text = getString(R.string.home_found_pins, pins.size)
                visibility = View.VISIBLE
            }
        } else {
            binding.emptyHintContainer.visibility = View.VISIBLE
        }
    }

    private fun addMenuProvider() {
        (requireActivity() as MenuHost).addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_home, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_analysis -> {
                            val action = HomeFragmentDirections.homeToAnalysis()
                            findNavController().navigate(action)
                            true
                        }
                        R.id.action_github -> {
                            openUrl(getString(R.string.github_link), "PINcredible GitHub")
                            true
                        }
                        R.id.action_about -> {
                            showAboutDialog()
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun showAboutDialog() {
        myContext.showDialog(
            getString(R.string.dialog_about_title),
            getString(
                R.string.dialog_about_message,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                BuildConfig.BUILD_TYPE,
                Build.MANUFACTURER,
                Build.MODEL,
                Build.DEVICE,
                when (Build.VERSION.SDK_INT) {
                    19, 20 -> "4"
                    21, 22 -> "5"
                    23 -> "6"
                    24, 25 -> "7"
                    26, 27 -> "8"
                    28 -> "9"
                    29 -> "10"
                    30 -> "11"
                    31, 32 -> "12"
                    33 -> "13"
                    else -> "> 13"
                },
                Build.VERSION.SDK_INT
            ),
            R.drawable.colored_ic_information,
            { showIconCreditsDialog() },
            getString(R.string.dialog_about_button)
        )
    }

    private fun showIconCreditsDialog() {
        myContext.showDialog(
            getString(R.string.dialog_credits_title),
            getString(R.string.dialog_credits_message),
            R.drawable.colored_ic_information,
            { openUrl("https://flaticon.com", "Flaticon") },
            getString(R.string.dialog_credits_button)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
