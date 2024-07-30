package de.tfelix.evmbitstream.util

fun ByteArray.toHex(): String = "0x" + joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun concatByteArrays(arrays: List<ByteArray>): ByteArray {
    val len = arrays.sumOf { it.size }
    val result = ByteArray(len)

    var lengthSoFar = 0
    for (array in arrays) {
        System.arraycopy(array, 0, result, lengthSoFar, array.size)
        lengthSoFar += array.size
    }

    return result
}