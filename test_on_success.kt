fun main() {
    val result = Result.success("test")
    result.onSuccess {
        throw RuntimeException("Crash from inside onSuccess")
    }.onFailure {
        println("Caught in onFailure")
    }
}
