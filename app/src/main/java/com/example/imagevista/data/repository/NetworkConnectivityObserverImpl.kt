package com.example.imagevista.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.imagevista.domain.model.NetworkStatus
import com.example.imagevista.domain.repository.NetworkConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class NetworkConnectivityObserverImpl(
    context: Context, scope: CoroutineScope
) : NetworkConnectivityObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkStatus = MutableStateFlow(NetworkStatus.Disconnected)
    override val networkStatus: StateFlow<NetworkStatus> get() = _networkStatus

    init {
        observeNetworkStatus()
            .distinctUntilChanged()
            .onEach { status ->
                _networkStatus.value = status
            }
            .launchIn(scope)
    }

    private fun observeNetworkStatus(): Flow<NetworkStatus> {
        return callbackFlow {
            val connectivityCallback = object : NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(NetworkStatus.Connected).isSuccess
                }

                override fun onLost(network: Network) {
                    trySend(NetworkStatus.Disconnected).isSuccess
                }
            }

            val request =
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build()

            connectivityManager.registerNetworkCallback(request, connectivityCallback)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(connectivityCallback)
            }
        }
    }
}
