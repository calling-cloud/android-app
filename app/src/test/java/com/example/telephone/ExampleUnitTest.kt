package com.example.telephone

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun networkErrorMessage_describesTimeout() {
        assertEquals("请求超时，请稍后重试", networkErrorMessage(SocketTimeoutException()))
    }

    @Test
    fun networkErrorMessage_describesUnavailableServer() {
        val expected = "无法连接服务器，请检查网络或稍后重试"
        assertEquals(expected, networkErrorMessage(UnknownHostException()))
        assertEquals(expected, networkErrorMessage(ConnectException()))
        assertEquals(expected, networkErrorMessage(NoRouteToHostException()))
    }

    @Test
    fun networkErrorMessage_describesOtherNetworkFailures() {
        assertEquals("网络请求失败，请检查网络连接", networkErrorMessage(IOException()))
    }

    @Test
    fun serverErrorMessage_includesHttpStatus() {
        assertEquals("服务器异常（500），请稍后重试", serverErrorMessage(500))
    }
}
