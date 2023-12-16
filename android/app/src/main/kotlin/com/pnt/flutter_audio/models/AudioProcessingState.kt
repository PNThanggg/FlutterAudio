package com.pnt.flutter_audio.models

enum class AudioProcessingState {
    IDLE,
    LOADING,
    BUFFERING,
    READY,
    COMPLETED,
    ERROR,
}