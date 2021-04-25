package com.cafebazaarreactnativepoolakey

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.exception.DisconnectException

class ReactNativePoolakeyModule(
  private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return "ReactNativePoolakey"
  }

  lateinit var payment: Payment
  var paymentConnection: Connection? = null

  @ReactMethod
  fun initializePayment(rsaPublicKey: String? = null) {

    val securityCheck = if (rsaPublicKey == null) {
      SecurityCheck.Disable
    } else {
      SecurityCheck.Enable(rsaPublicKey = rsaPublicKey)
    }
    val paymentConfig = PaymentConfiguration(localSecurityCheck = securityCheck)
    payment = Payment(context = reactContext, config = paymentConfig)
  }

  @ReactMethod
  fun connectPayment(promise: Promise) {
    runIfPaymentInitialized(promise) {
      paymentConnection = payment.connect {
        connectionSucceed { promise.resolve(null) }
        connectionFailed { promise.reject(it) }
        disconnected { promise.reject(DisconnectException()) }
      }
    }
  }

  @ReactMethod
  fun disconnectPayment(promise: Promise) {
    paymentConnection?.disconnect()
    promise.resolve(null)
  }

  private fun runIfPaymentInitialized(promise: Promise, runner: () -> Unit) {
    if (::payment.isInitialized) {
      promise.reject(IllegalStateException("payment not initialized"))
      return
    }

    runner.invoke()
  }
}
