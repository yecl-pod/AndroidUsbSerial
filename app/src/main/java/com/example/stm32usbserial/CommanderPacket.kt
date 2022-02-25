package com.example.stm32usbserial

import java.nio.ByteBuffer

class CommanderPacket(roll: Float, pitch: Float, yaw: Float, thrust: UShort): CrtpPacket(0, CrtpPort.COMMANDER) {
    private var mRoll: Float = roll // +left/-right
    private var mPitch: Float = pitch // +forward/-backward
    private var mYaw: Float = yaw // +rotate clockwise/-counter clockwise
    private var mThrust: UShort = thrust // multiplier
    
    // make sure (mRoll + mPitch) * mThrust < 14000

    override fun getDataByteCount(): Int {
        return (3 * 4 + 1 * 2)
    }

    override fun serializeData(b: ByteBuffer) {
        b.putFloat(mRoll)
        b.putFloat(mPitch)
        b.putFloat(mYaw)
        b.putShort(mThrust.toShort())
    }

    override fun toString(): String {
        return "CommanderPacket - R: $mRoll, P: $mPitch, Y: $mYaw, T: $mThrust"
    }
}
