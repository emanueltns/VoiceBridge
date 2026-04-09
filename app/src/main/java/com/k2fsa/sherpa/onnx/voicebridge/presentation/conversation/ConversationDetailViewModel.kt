package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Message
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationDetailState(
    val title: String = "",
    val messages: List<Message> = emptyList(),
)

@HiltViewModel
class ConversationDetailViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConversationDetailState())
    val state: StateFlow<ConversationDetailState> = _state.asStateFlow()

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            val conversation = conversationRepository.getConversation(conversationId)
            _state.update { it.copy(title = conversation?.title ?: "Conversation") }
        }
        viewModelScope.launch {
            conversationRepository.getMessages(conversationId).collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }
    }
}
