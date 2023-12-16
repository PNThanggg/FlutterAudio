package com.pnt.flutter_audio.models

class MediaControl(
    val icon: String,
    val label: String,
    val actionCode: Long,
    val customAction: CustomMediaAction
) {
    override fun equals(other: Any?): Boolean {
        return if (other is MediaControl) {
            icon == other.icon && label == other.label && actionCode == other.actionCode && customAction == other.customAction
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = icon.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + actionCode.hashCode()
        result = 31 * result + customAction.hashCode()
        return result
    }
}