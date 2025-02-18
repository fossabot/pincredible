/*
 * Copyright (c) 2023 Cyb3rKo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyb3rko.pincredible.data

import com.cyb3rko.pincredible.BuildConfig
import com.cyb3rko.pincredible.crypto.CryptoManager

internal class PinTable {
    private lateinit var data: Array<IntArray>
    lateinit var pattern: Array<IntArray>

    init {
        reset()
    }

    fun reset() {
        data = Array(ROW_COUNT) { IntArray(COLUMN_COUNT) { -1 } }
    }

    fun isFilled(): Boolean {
        data.forEach {
            if (it.contains(-1)) return false
        }
        return true
    }

    fun put(row: Int, column: Int, value: Int) {
        if (row < 0 || row > ROW_COUNT) throw IllegalArgumentException("Invalid row index")
        if (column < 0 || column > COLUMN_COUNT) throw IllegalArgumentException("Invalid row index")
        data[row][column] = value
    }

    fun get(row: Int, column: Int) = data[row][column].toString()

    fun getBackground(row: Int, column: Int) = pattern[row][column]

    fun fill() {
        data.forEach {
            it.forEachIndexed { index, i ->
                if (i == -1) {
                    it[index] = CryptoManager.getSecureRandom()
                }
            }
        }
    }

    private fun getPinSequence(): String {
        var output = ""
        data.forEach { array ->
            array.forEach {
                output += it
            }
        }
        return output
    }

    private fun getPatternSequence(): String {
        var output = ""
        pattern.forEach { array ->
            array.forEach {
                output += it
            }
        }
        return output
    }

    fun getData(): ByteArray {
        return (
            BuildConfig.VERSION_CODE.toString() + "-" + getPatternSequence() + "-" +
                getPinSequence()
            ).toByteArray()
    }

    companion object {
        const val ROW_COUNT = 7
        const val COLUMN_COUNT = 7

        fun extractData(rawData: ByteArray): Triple<Int, String, String> {
            val parts = rawData.decodeToString().split("-")
            return Triple(parts[0].toInt(), parts[1], parts[2])
        }
    }
}
