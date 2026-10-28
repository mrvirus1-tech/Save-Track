package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SAVE TRACK", appName)
  }

  @Test
  fun `verify default user role is CUSTOMER`() {
    val user = com.example.data.model.User()
    assertEquals("CUSTOMER", user.role)
    assertEquals("ACTIVE", user.status)
  }

  @Test
  fun `verify parcel initial status is CREATED`() {
    val parcel = com.example.data.model.Parcel(title = "Electronics", trackingNumber = "ST-1234")
    assertEquals("CREATED", parcel.status)
    assertEquals("NORMAL", parcel.priority)
  }
}
