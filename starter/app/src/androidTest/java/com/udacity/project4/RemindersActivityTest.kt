package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var _viewModel: SaveReminderViewModel


    val testReminder = ReminderDTO(
        "Title Test",
        "Description Test",
        "Location Test",
        0.0,
        0.0,
        45F
    )

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }


    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }

             viewModel { SelectLocationViewModel() }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }


    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun launchRemindersActivityWithOneReminder() {
        val reminder = ReminderDTO(
            "Test",
            "testing",
            "Testing",
            2.89893,
            1.98893,
            100f
        )

        runBlocking {
            repository.saveReminder(reminder)
        }

        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)

        onView(withText(reminder.title)).check(matches(isDisplayed()))
        onView(withText(reminder.description)).check(matches(isDisplayed()))
        onView(withText(reminder.location)).check(matches(isDisplayed()))
    }

    @Test
    fun addReminderAndNavigateBack() {
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)

        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())

        onView(withId(R.id.fragment_container)).perform(click())
        onView(withId(R.id.save_button)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("Title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("Description"))

        Espresso.closeSoftKeyboard()

        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withText("Title")).check(matches(isDisplayed()))
        onView(withText("Description")).check(matches(isDisplayed()))
    }

    @Test
    fun reminderList_enter_text_and_location() {

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.reminderTitle))
            .perform(ViewActions.replaceText("Test Title"))
        Espresso.onView(ViewMatchers.withId(R.id.reminderDescription))
            .perform(ViewActions.replaceText("Test Description"))
        //Just to see screen
        Thread.sleep(1000)


        //Just to see screen
        Thread.sleep(1000)
        onView(withId(R.id.save_button)).perform(ViewActions.click())
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        onView(withText("Test Title"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Test Description"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        activityScenario.close()
    }

    @Test
    fun remindersActivityTest_fragmentSaveReminder() = runBlocking {

        repository.saveReminder(testReminder)

        _viewModel = inject<SaveReminderViewModel>().value

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.reminderssRecyclerView)).check((matches(isDisplayed())))

        onView(withId(R.id.addReminderFAB)).perform(click())

        onView(withId(R.id.saveReminderFragment)).check((matches(isDisplayed())))

        onView(withId(R.id.reminderTitle)).perform(clearText(), typeText("Have a hamburger..."))
        onView(withId(R.id.reminderTitle)).check(matches(withText("Have a hamburger...")))
        onView(withId(R.id.reminderDescription)).perform(clearText(), typeText("While we are testing..."))
        onView(withId(R.id.reminderDescription)).check(matches(withText("While we are testing...")))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.fragment_select_location)).check((matches(isDisplayed())))
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.select_poi)))
        Espresso.pressBack()
        onView(withId(R.id.saveReminderFragment)).check((matches(isDisplayed())))
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(R.id.saveReminderFragment)).check((matches(isDisplayed())))
        Espresso.pressBack()
        onView(withId(R.id.reminderssRecyclerView)).check((matches(isDisplayed())))
        activityScenario.close()
    }

    @Test
    fun setError_AddRemindersFirstMessageIsDisplayed() {

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.reminderssRecyclerView)).check((matches(isDisplayed())))

        onView(withId(R.id.refreshLayout)).perform(swipeDown())

        val daErrorText = getActivity(activityScenario)?.getString(R.string.error_add_reminders)
        onView(withText(daErrorText)).inRoot(
            RootMatchers.withDecorView(
                CoreMatchers.not(
                    CoreMatchers.`is`(getActivity(activityScenario)?.window?.decorView)
                )
            )
        )
            .check(matches(isDisplayed()))

        activityScenario.close()
    }
}
