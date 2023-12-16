package com.pnt.flutter_audio.models

import java.util.Objects

class CustomMediaAction(val name: String, val extras: Map<*, *>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as CustomMediaAction
        return name == that.name && extras == that.extras
    }

    override fun hashCode(): Int {
        return Objects.hash(name, extras)
    }
}
