package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    private lateinit var datasource: ReminderDataSource
    private lateinit var appContext: Application

    val testReminder = ReminderDTO(
        "Title Test",
        "Description Test",
        "Location Test",
        0.0,
        0.0
    )

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        stopKoin()
        appContext = getApplicationContext()
        datasource = FakeDataSource()
        val myModules = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    datasource as ReminderDataSource
                )
            }
        }

        startKoin {
            modules(listOf(myModules))
        }

        runBlocking {
            datasource.deleteAllReminders()
            datasource.saveReminder(testReminder)
        }
    }

    @Test
    fun emptyReminderScreen_display_error_text() {
        runBlocking {
            datasource.deleteAllReminders()
        }
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)

        scenario.onFragment { Navigation.setViewNavController(it.view!!, navController) }

        Espresso.onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(
            withText(
                appContext
                    .getString(R.string.no_data)
            )
        )
            .check(ViewAssertions.matches(isDisplayed()))
    }

    @Test
    fun savedReminders_display_on_screen() {
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        Espresso.onView(withText(testReminder.title)).check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(withText(testReminder.description))
            .check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(withText(testReminder.location))
            .check(ViewAssertions.matches(isDisplayed()))
    }

    @Test
    fun fabNavigation_click_navigates_to_new_reminder() {
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB)).perform(ViewActions.click())
        Mockito.verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }
}