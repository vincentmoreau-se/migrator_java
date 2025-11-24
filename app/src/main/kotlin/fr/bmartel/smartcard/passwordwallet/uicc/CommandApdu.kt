package fr.bmartel.smartcard.passwordwallet.uicc

/*
 * Copyright 2010 Giesecke & Devrient GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * class needed for compatibility between open mobile API versions.
 */
class CommandApdu {

    protected var mCla: Int = 0x00
    protected var mIns: Int = 0x00
    protected var mP1: Int = 0x00
    protected var mP2: Int = 0x00
    protected var mLc: Int = 0x00
    protected var mData: ByteArray = byteArrayOf()
    protected var mLe: Int = 0x00
    protected var mLeUsed: Boolean = false

    constructor()

    constructor(cla: Int, ins: Int, p1: Int, p2: Int) {
        mCla = cla
        mIns = ins
        mP1 = p1
        mP2 = p2
    }

    constructor(cla: Int, ins: Int, p1: Int, p2: Int, data: ByteArray) {
        mCla = cla
        mIns = ins
        mLc = data.size
        mP1 = p1
        mP2 = p2
        mData = data
    }

    constructor(cla: Int, ins: Int, p1: Int, p2: Int, data: ByteArray, le: Int) {
        mCla = cla
        mIns = ins
        mLc = data.size
        mP1 = p1
        mP2 = p2
        mData = data
        mLe = le
        mLeUsed = true
    }

    constructor(cla: Int, ins: Int, p1: Int, p2: Int, le: Int) {
        mCla = cla
        mIns = ins
        mP1 = p1
        mP2 = p2
        mLe = le
        mLeUsed = true
    }

    fun setP1(p1: Int) {
        mP1 = p1
    }

    fun setP2(p2: Int) {
        mP2 = p2
    }

    fun setData(data: ByteArray) {
        mLc = data.size
        mData = data
    }

    fun setLe(le: Int) {
        mLe = le
        mLeUsed = true
    }

    fun getP1(): Int = mP1

    fun getP2(): Int = mP2

    fun getLc(): Int = mLc

    fun getData(): ByteArray = mData

    fun getLe(): Int = mLe

    fun toBytes(): ByteArray {
        var length = 4 // CLA, INS, P1, P2
        if (mData.isNotEmpty()) {
            length += 1 // LC
            length += mData.size // DATA
        }
        if (mLeUsed) {
            length += 1 // LE
        }

        val apdu = ByteArray(length)

        var index = 0
        apdu[index++] = mCla.toByte()
        apdu[index++] = mIns.toByte()
        apdu[index++] = mP1.toByte()
        apdu[index++] = mP2.toByte()

        if (mData.isNotEmpty()) {
            apdu[index++] = mLc.toByte()
            System.arraycopy(mData, 0, apdu, index, mData.size)
            index += mData.size
        }
        if (mLeUsed) {
            apdu[index] = (apdu[index] + mLe.toByte()).toByte() // LE
        }

        return apdu
    }

    fun clone(): CommandApdu {
        val apdu = CommandApdu()
        apdu.mCla = mCla
        apdu.mIns = mIns
        apdu.mP1 = mP1
        apdu.mP2 = mP2
        apdu.mLc = mLc
        apdu.mData = mData.copyOf()
        apdu.mLe = mLe
        apdu.mLeUsed = mLeUsed
        return apdu
    }

    companion object {
        @JvmStatic
        fun compareHeaders(header1: ByteArray, mask: ByteArray, header2: ByteArray): Boolean {
            if (header1.size < 4 || header2.size < 4) {
                return false
            }
            val compHeader = ByteArray(4)
            compHeader[0] = (header1[0].toInt() and mask[0].toInt()).toByte()
            compHeader[1] = (header1[1].toInt() and mask[1].toInt()).toByte()
            compHeader[2] = (header1[2].toInt() and mask[2].toInt()).toByte()
            compHeader[3] = (header1[3].toInt() and mask[3].toInt()).toByte()

            return compHeader[0] == header2[0] &&
                   compHeader[1] == header2[1] &&
                   compHeader[2] == header2[2] &&
                   compHeader[3] == header2[3]
        }
    }
}
