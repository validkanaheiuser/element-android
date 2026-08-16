/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.homeserver.LockedHomeserverStore
import im.vector.app.core.homeserver.ServerConfig
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentLoginServerSelectionBinding
import im.vector.app.features.login.EMS_LINK
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings
import me.gujun.android.span.span
import javax.inject.Inject

/**
 * In this screen, the user will choose between matrix.org, modular or other type of homeserver.
 */
@AndroidEntryPoint
class FtueAuthServerSelectionFragment :
        AbstractFtueAuthFragment<FragmentLoginServerSelectionBinding>() {

    @Inject lateinit var lockedHomeserverStore: LockedHomeserverStore

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginServerSelectionBinding {
        return FragmentLoginServerSelectionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val servers = lockedHomeserverStore.getServerList()
        if (servers.isNotEmpty()) {
            if (servers.size == 1) {
                lockedHomeserverStore.setSelectedUrl(servers[0].url)
                viewModel.handle(OnboardingAction.HomeServerChange.EditHomeServer(servers[0].url))
                return
            }
            setupServerSpinner(servers)
            return
        }

        initViews()
        initTextViews()
    }

    private fun initViews() {
        views.loginServerChoiceEmsLearnMore.setOnClickListener { learnMore() }
        views.loginServerChoiceMatrixOrg.setOnClickListener { selectMatrixOrg() }
        views.loginServerChoiceEms.setOnClickListener { selectEMS() }
        views.loginServerChoiceOther.setOnClickListener { selectOther() }
        views.loginServerIKnowMyIdSubmit.setOnClickListener { loginWithMatrixId() }
    }

    private fun updateSelectedChoice(state: OnboardingViewState) {
        views.loginServerChoiceMatrixOrg.isChecked = state.serverType == ServerType.MatrixOrg
    }

    private fun initTextViews() {
        views.loginServerChoiceEmsLearnMore.text = span {
            text = getString(CommonStrings.login_server_modular_learn_more)
            textDecorationLine = "underline"
        }
    }

    private fun learnMore() {
        openUrlInChromeCustomTab(requireActivity(), null, EMS_LINK)
    }

    private fun selectMatrixOrg() {
        viewModel.handle(OnboardingAction.UpdateServerType(ServerType.MatrixOrg))
    }

    private fun selectEMS() {
        viewModel.handle(OnboardingAction.UpdateServerType(ServerType.EMS))
    }

    private fun selectOther() {
        viewModel.handle(OnboardingAction.UpdateServerType(ServerType.Other))
    }

    private fun loginWithMatrixId() {
        viewModel.handle(OnboardingAction.UpdateSignMode(SignMode.SignInWithMatrixId))
    }

    private fun setupServerSpinner(servers: List<ServerConfig>) {
        views.loginServerChoiceMatrixOrg.visibility = View.GONE
        views.loginServerChoiceEms.visibility = View.GONE
        views.loginServerChoiceOther.visibility = View.GONE

        val nicknames = servers.map { it.nickname }.toTypedArray()
        var selectedIndex = servers.indexOfFirst { it.url == lockedHomeserverStore.getSelectedUrl() }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Server")
            .setSingleChoiceItems(nicknames, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Continue") { _, _ ->
                val selected = servers[selectedIndex]
                lockedHomeserverStore.setSelectedUrl(selected.url)
                viewModel.handle(OnboardingAction.HomeServerChange.EditHomeServer(selected.url))
            }
            .setCancelable(false)
            .show()
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetHomeServerType)
    }

    override fun updateWithState(state: OnboardingViewState) {
        updateSelectedChoice(state)
    }
}
