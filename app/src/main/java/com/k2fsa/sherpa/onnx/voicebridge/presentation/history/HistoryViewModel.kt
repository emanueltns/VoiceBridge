package com.k2fsa.sherpa.onnx.voicebridge.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Conversation
import com.k2fsa.sherpa.onnx.voicebridge.domain.usecase.DeleteConversationUseCase
import com.k2fsa.sherpa.onnx.voicebridge.domain.usecase.GetConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val conversations: List<Conversation> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getConversations: GetConversationsUseCase,
    private val deleteConversation: DeleteConversationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getConversations().collect { conversations ->
                _state.update { it.copy(conversations = conversations) }
            }
        }
    }

    fun onDelete(conversationId: String) {
        viewModelScope.launch {
            deleteConversation(conversationId)
        }
    }
}
