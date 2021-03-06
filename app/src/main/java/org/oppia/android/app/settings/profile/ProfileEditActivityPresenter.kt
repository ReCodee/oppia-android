package org.oppia.android.app.settings.profile

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import org.oppia.android.R
import org.oppia.android.app.activity.ActivityScope
import org.oppia.android.app.model.ProfileId
import org.oppia.android.databinding.ProfileEditActivityBinding
import org.oppia.android.domain.profile.ProfileManagementController
import org.oppia.android.util.data.DataProviders.Companion.toLiveData
import org.oppia.android.util.logging.ConsoleLogger
import javax.inject.Inject

/** The presenter for [ProfileEditActivity]. */
@ActivityScope
class ProfileEditActivityPresenter @Inject constructor(
  private val activity: AppCompatActivity,
  private val logger: ConsoleLogger,
  private val profileManagementController: ProfileManagementController
) {

  @Inject
  lateinit var profileEditViewModel: ProfileEditViewModel

  fun handleOnCreate() {
    activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    activity.supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp)

    val binding = DataBindingUtil.setContentView<ProfileEditActivityBinding>(
      activity,
      R.layout.profile_edit_activity
    )
    val profileId = activity.intent.getIntExtra(KEY_PROFILE_EDIT_PROFILE_ID, 0)
    profileEditViewModel.setProfileId(profileId)

    binding.apply {
      viewModel = profileEditViewModel
      lifecycleOwner = activity
    }

    binding.profileRenameButton.setOnClickListener {
      activity.startActivity(ProfileRenameActivity.createProfileRenameActivity(activity, profileId))
    }

    binding.profileResetButton.setOnClickListener {
      activity.startActivity(
        ProfileResetPinActivity.createProfileResetPinActivity(
          activity,
          profileId,
          profileEditViewModel.isAdmin
        )
      )
    }

    binding.profileDeleteButton.setOnClickListener {
      showDeletionDialog(profileId)
    }

    profileEditViewModel.profile.observe(
      activity,
      Observer {
        activity.title = it.name
      }
    )

    profileEditViewModel.isAllowedDownloadAccess.observe(
      activity,
      Observer {
        binding.profileEditAllowDownloadSwitch.isChecked = it
      }
    )

    binding.profileEditAllowDownloadSwitch.setOnCheckedChangeListener { compoundButton, checked ->
      if (compoundButton.isPressed) {
        profileManagementController.updateAllowDownloadAccess(
          ProfileId.newBuilder().setInternalId(profileId).build(),
          checked
        ).toLiveData().observe(
          activity,
          Observer {
            if (it.isFailure()) {
              logger.e(
                "ProfileEditActivityPresenter",
                "Failed to updated allow download access",
                it.getErrorOrNull()!!
              )
            }
          }
        )
      }
    }
  }

  private fun showDeletionDialog(profileId: Int) {
    AlertDialog.Builder(activity, R.style.AlertDialogTheme)
      .setTitle(R.string.profile_edit_delete_dialog_title)
      .setMessage(R.string.profile_edit_delete_dialog_message)
      .setNegativeButton(R.string.profile_edit_delete_dialog_negative) { dialog, _ ->
        dialog.dismiss()
      }
      .setPositiveButton(R.string.profile_edit_delete_dialog_positive) { dialog, _ ->
        profileManagementController
          .deleteProfile(ProfileId.newBuilder().setInternalId(profileId).build()).toLiveData()
          .observe(
            activity,
            Observer {
              if (it.isSuccess()) {
                val intent = Intent(activity, ProfileListActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                activity.startActivity(intent)
              }
            }
          )
      }.create().show()
  }
}
