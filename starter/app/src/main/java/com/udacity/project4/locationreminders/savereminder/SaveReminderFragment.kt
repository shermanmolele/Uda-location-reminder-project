package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.LocationUtils
import com.udacity.project4.utils.PermissionsResultEvent
import com.udacity.project4.utils.dp
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    companion object {
        private const val TAG = "SaveReminderFragment"
    }

    override val viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSaveReminderBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectLocationLayout.setOnClickListener {
            viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        ViewCompat.setElevation(binding.progressBar, 100.dp)

        binding.saveReminder.setOnClickListener {
            val title = viewModel.reminderTitle.value
            val description = viewModel.reminderDescription.value

            val poi = viewModel.selectedPlaceOfInterest.value
            val latitude = poi?.latLng?.latitude
            val longitude = poi?.latLng?.longitude
            val radius = viewModel.selectedRadius.value

            val reminderData = ReminderDataItem(title, description, poi?.name, latitude, longitude, radius)

            if (viewModel.validateAndSaveReminder(reminderData)) {
                startAtCurrentLocation()
                addGeofence(reminderData)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(reminderData: ReminderDataItem) {
        val geofence = Geofence.Builder()
            .setRequestId(reminderData.id)
            .setCircularRegion(
                reminderData.latitude!!,
                reminderData.longitude!!,
                reminderData.radius!!
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = GeofenceConstants.ACTION_GEOFENCE_EVENT

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val client = LocationServices.getGeofencingClient(requireContext())

        client.addGeofences(request, pendingIntent)?.run {
            addOnSuccessListener {
                Log.d(TAG, "Added geofence for reminder with id ${reminderData.id} successfully.")
            }
            addOnFailureListener {
                viewModel.showErrorMessage.postValue(context?.getString(R.string.error_adding_geofence))
                it.message?.let { message ->
                    Log.w(TAG, message)
                }
            }
        }
    }
    private fun locationPermissionHandler(event: PermissionsResultEvent, handler: () -> Unit) {
        if (event.areAllGranted) {
            handler()
            return
        }

        if (event.shouldShowRequestRationale) {
            viewModel.showSnackBar.postValue(getString(R.string.permission_denied_explanation))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAtCurrentLocation() {
        if (!LocationUtils.hasLocationPermissions()) {
            LocationUtils.requestPermissions {
                locationPermissionHandler(it, this::startAtCurrentLocation)
            }

            return
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        viewModel.onClear()
    }
}
