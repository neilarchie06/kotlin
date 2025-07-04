// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FULL_JDK

import java.time.LocalDate

data class DailyTime(val date: LocalDate)

fun <T : Comparable<T>> Sequence<T>.range(): ClosedRange<T>? {
    val iter = iterator()
    return when {
        iter.hasNext() -> iter.next().let { it..it }
        else -> null
    }
}

fun test(dailyTimes: List<DailyTime>): List<DailyTime> {
    val dateRange = dailyTimes.asSequence().map { it.date }.range() ?: return emptyList()
    println(dateRange.start)
    return dailyTimes
}

/* GENERATED_FIR_TAGS: classDeclaration, data, elvisExpression, funWithExtensionReceiver, functionDeclaration,
lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, rangeExpression, typeConstraint,
typeParameter, whenExpression */
