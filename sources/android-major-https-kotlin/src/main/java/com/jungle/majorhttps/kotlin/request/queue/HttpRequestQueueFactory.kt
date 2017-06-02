package com.jungle.majorhttps.kotlin.request.queue

import android.content.Context
import android.support.annotation.RawRes
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.jungle.majorhttps.kotlin.network.HttpsUtils
import java.security.cert.Certificate
import java.util.*
import javax.net.ssl.HostnameVerifier


class HttpRequestQueueFactory(private val mContext: Context) : RequestQueueFactory {

    override fun createRequestQueue(): RequestQueue {
        return Volley.newRequestQueue(mContext)
    }
}


class HttpsRequestQueueFactory : RequestQueueFactory {

    companion object {

        fun create(
                context: Context, certAssetName: String, vararg domains: String)
                : HttpsRequestQueueFactory {

            val factory = HttpsRequestQueueFactory(context, arrayOf(certAssetName))
            factory.setHostnameVerifier(HttpsUtils.DomainHostnameVerifier(domains))
            return factory
        }

        fun create(
                context: Context, @RawRes certRawResId: Int, vararg domains: String)
                : HttpsRequestQueueFactory {

            val factory = HttpsRequestQueueFactory(context, certRawResId)
            factory.setHostnameVerifier(HttpsUtils.DomainHostnameVerifier(domains))
            return factory
        }
    }


    private lateinit var mContext: Context
    private var mHostnameVerifier: HostnameVerifier? = null
    private val mCertificateList = ArrayList<Certificate>()


    constructor(context: Context) {
        mContext = context
    }

    constructor(context: Context, @RawRes vararg certRawResIds: Int) {
        mContext = context
        for (certRawResId in certRawResIds) {
            val cert = HttpsUtils.createCertificateByRawResource(mContext, certRawResId)
            if (cert != null) {
                mCertificateList.add(cert)
            }
        }
    }

    constructor(context: Context, certAssetNames: Array<String>) {
        mContext = context
        certAssetNames.mapNotNullTo(mCertificateList) {
            HttpsUtils.createCertificateByCrtAsset(mContext, it)
        }
    }

    constructor(context: Context, certs: Array<Certificate>) {
        mContext = context
        mCertificateList.addAll(certs)
    }

    constructor(context: Context, list: List<Certificate>) {
        mContext = context
        mCertificateList.addAll(list)
    }

    fun setHostnameVerifier(hostnameVerifier: HostnameVerifier) {
        mHostnameVerifier = hostnameVerifier
    }

    override fun createRequestQueue(): RequestQueue {
        if (mHostnameVerifier == null) {
            mHostnameVerifier = HttpsUtils.DefaultHostnameVerifier()
        }

        var stack: VolleyHttpsStack? = null
        if (!mCertificateList.isEmpty()) {
            val certs = mCertificateList.toTypedArray()
            val sslContext = HttpsUtils.getSSLContext(
                    HttpsUtils.createTrustManagerByCerts(certs))

            if (sslContext != null) {
                val factory = sslContext.socketFactory
                stack = VolleyHttpsStack(null, factory, mHostnameVerifier)
            }
        }

        return Volley.newRequestQueue(mContext, stack)
    }
}
