package com.pnt.flutter_audio.interfaces

interface PermissionManagerInterface {
    fun permissionStatus() : Boolean
    fun requestPermission()
    fun retryRequestPermission()
}