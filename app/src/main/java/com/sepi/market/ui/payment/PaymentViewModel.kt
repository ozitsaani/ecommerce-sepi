package com.sepi.market.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sepi.market.core.ApiResult
import com.sepi.market.core.Formatters
import com.sepi.market.core.UiState
import com.sepi.market.data.remote.dto.OrderDto
import com.sepi.market.data.repository.OrderRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PaymentUiEvent {
    data class PaymentSuccess(val order: OrderDto) : PaymentUiEvent
    data class OrderCancelled(val order: OrderDto) : PaymentUiEvent
    data class ShowToast(val message: String) : PaymentUiEvent
    data class RedirectToDetail(val orderId: String) : PaymentUiEvent
}

class PaymentViewModel(
    val orderRepository: OrderRepository
) : ViewModel() {

    private val _orderState = MutableStateFlow<UiState<OrderDto>>(UiState.Loading)
    val orderState: StateFlow<UiState<OrderDto>> = _orderState.asStateFlow()

    private val _event = MutableSharedFlow<PaymentUiEvent>(extraBufferCapacity = 8)
    val event: SharedFlow<PaymentUiEvent> = _event.asSharedFlow()

    val countdownText = MutableStateFlow("00:00:00")
    val isExpired = MutableStateFlow(false)
    val isActionLoading = MutableStateFlow(false)

    private var countdownJob: Job? = null

    fun loadOrderDetail(orderId: String) {
        _orderState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = orderRepository.getOrderDetail(orderId)) {
                is ApiResult.Success -> {
                    val order = result.data
                    if (order != null) {
                        if (order.status != "UNPAID") {
                            _event.tryEmit(PaymentUiEvent.RedirectToDetail(order.id))
                        } else {
                            _orderState.value = UiState.Success(order)
                            startCountdown(order.paymentDeadline)
                        }
                    } else {
                        _orderState.value = UiState.Error("Pesanan tidak ditemukan")
                    }
                }
                is ApiResult.Error -> {
                    _orderState.value = UiState.Error(result.message)
                }
            }
        }
    }

    private fun startCountdown(deadlineIso: String) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val cd = Formatters.countdown(deadlineIso)
                countdownText.value = cd
                if (cd == "00:00:00") {
                    isExpired.value = true
                    break
                }
                delay(1000)
            }
        }
    }

    fun checkPaymentStatus(orderId: String) {
        isActionLoading.value = true
        viewModelScope.launch {
            when (val result = orderRepository.simulatePayment(orderId)) {
                is ApiResult.Success -> {
                    isActionLoading.value = false
                    _event.tryEmit(PaymentUiEvent.PaymentSuccess(result.data))
                }
                is ApiResult.Error -> {
                    isActionLoading.value = false
                    _event.tryEmit(PaymentUiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun cancelOrder(orderId: String) {
        isActionLoading.value = true
        viewModelScope.launch {
            when (val result = orderRepository.cancelOrder(orderId)) {
                is ApiResult.Success -> {
                    isActionLoading.value = false
                    _event.tryEmit(PaymentUiEvent.OrderCancelled(result.data))
                }
                is ApiResult.Error -> {
                    isActionLoading.value = false
                    _event.tryEmit(PaymentUiEvent.ShowToast(result.message))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
