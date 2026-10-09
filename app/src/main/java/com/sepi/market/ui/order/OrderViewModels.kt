package com.sepi.market.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sepi.market.core.ApiResult
import com.sepi.market.core.UiState
import com.sepi.market.data.remote.dto.OrderDto
import com.sepi.market.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrdersViewModel(
    val orderRepository: OrderRepository
) : ViewModel() {

    private val _ordersState = MutableStateFlow<UiState<List<OrderDto>>>(UiState.Loading)
    val ordersState: StateFlow<UiState<List<OrderDto>>> = _ordersState.asStateFlow()

    private var lastLoadedFilter: String? = "UNSET"

    fun loadOrders(statusFilter: String? = null, forceRefresh: Boolean = false) {
        if (!forceRefresh && lastLoadedFilter == statusFilter && _ordersState.value is UiState.Success) {
            return
        }
        lastLoadedFilter = statusFilter
        _ordersState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = orderRepository.getOrders(statusFilter)) {
                is ApiResult.Success -> {
                    _ordersState.value = UiState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _ordersState.value = UiState.Error(result.message)
                }
            }
        }
    }
}

sealed interface OrderDetailUiEvent {
    data class ShowToast(val message: String) : OrderDetailUiEvent
}

class OrderDetailViewModel(
    val orderRepository: OrderRepository
) : ViewModel() {

    private val _orderDetailState = MutableStateFlow<UiState<OrderDto>>(UiState.Loading)
    val orderDetailState: StateFlow<UiState<OrderDto>> = _orderDetailState.asStateFlow()

    private val _event = MutableSharedFlow<OrderDetailUiEvent>(extraBufferCapacity = 8)
    val event: SharedFlow<OrderDetailUiEvent> = _event.asSharedFlow()

    val isAdvancing = MutableStateFlow(false)

    private var lastOrderId: String = ""

    fun loadOrderDetail(orderId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && lastOrderId == orderId && _orderDetailState.value is UiState.Success) {
            return
        }
        lastOrderId = orderId
        _orderDetailState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = orderRepository.getOrderDetail(orderId)) {
                is ApiResult.Success -> {
                    val order = result.data
                    if (order != null) {
                        _orderDetailState.value = UiState.Success(order)
                    } else {
                        _orderDetailState.value = UiState.Error("Pesanan tidak ditemukan")
                    }
                }
                is ApiResult.Error -> {
                    _orderDetailState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun advanceOrder(orderId: String) {
        isAdvancing.value = true
        viewModelScope.launch {
            when (val result = orderRepository.advanceOrder(orderId)) {
                is ApiResult.Success -> {
                    isAdvancing.value = false
                    loadOrderDetail(orderId, forceRefresh = true)
                }
                is ApiResult.Error -> {
                    isAdvancing.value = false
                    _event.tryEmit(OrderDetailUiEvent.ShowToast(result.message))
                }
            }
        }
    }
}
