import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    // multiple columns
    val personsDf = dataFrameOf("name", "age", "city", "weight", "height", "yearsToRetirement")(
        "Alice", 15, "London", 99.5, "1.85", 50,
        "Bob", 20, "Paris", 140.0, "1.35", 45,
        "Charlie", 100, "Dubai", 75.0, "1.95", 0,
        "Rose", 1, "Moscow", 45.33, "0.79", 64,
        "Dylan", 35, "London", 23.4, "1.83", 30,
        "Eve", 40, "Paris", 56.72, "1.85", 25,
        "Frank", 55, "Dubai", 78.9, "1.35", 10,
        "Grace", 29, "Moscow", 67.8, "1.65", 36,
        "Hank", 60, "Paris", 80.22, "1.75", 5,
        "Isla", 22, "London", 75.1, "1.85", 43,
    )

    // scenario #0: all numerical columns
    val res0 = personsDf.groupBy { city }.std()
    val std01: Double? = res0.age[0]
    val std02: Double? = res0.weight[0]
    res0.compareSchemas()

    // scenario #1: particular column
    val res1 = personsDf.groupBy { city }.stdFor { age }
    val std11: Double? = res1.age[0]
    res1.compareSchemas()

    // scenario #1.1: particular column via std
    val res11 = personsDf.groupBy { city }.std { age }
    val std111: Double? = res11.age[0]
    res11.compareSchemas()

    // scenario #2: particular column with new name - schema changes
    // TODO: not supported scenario
    // val res2 = personsDf.groupBy { city }.std("age", name = "newAge")
    // val std21: Double? = res2.newAge[0]

    // scenario #2.1: particular column with new name - schema changes but via columnSelector
    val res21 = personsDf.groupBy { city }.std("newAge") { age }
    val std211: Double? = res21.newAge[0]
    res21.compareSchemas()

    // scenario #2.2: two columns with new name - schema changes but via columnSelector
    // TODO: handle multiple columns https://github.com/Kotlin/dataframe/issues/1090
    val res22 = personsDf.groupBy { city }.std("newAge") { age and yearsToRetirement }
    val std221: Double? = res22.newAge[0]
    res22.compareSchemas()

    // scenario #3: create new column via expression
    val res3 = personsDf.groupBy { city }.stdOf("newAge") { age * 10 }
    val std3: Double? = res3.newAge[0]
    res3.compareSchemas()

    return "OK"
}

