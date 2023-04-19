package dev.removingalldoubt.context.examples.mhtml

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import dev.removingalldoubt.context.key

class Person(name: String, age: Int) {
    var name by mutableStateOf(name)
    var age by mutableStateOf(age)
}

fun mhtmlExample() {
    val people = mutableStateListOf(
        Person("Bill Berkley", 32),
        Person("Carol Clark", 43),
        Person("Angela Allen", 23),
    )

    val doc = document {
        div {
            for (person in people) {
                key(person) {
                    PersonComponent(person)
                }
            }
        }
    }

    fun <T> update(block: () -> T): T {
        val result = block()
        Snapshot.sendApplyNotifications()
        doc.update()
        return result
    }

    try {
        println("Initial document ---")
        println(doc)

        update {
            people.add(Person("Dave Dickson", 24))
        }
        println("Added Dave ---------")
        println(doc)

        update {
            people.find { it.name.contains("Bill") }?.name = "William Berkley"
        }
        println("Updated Bill -------")
        println(doc)

        update {
            people.sortBy { it.name }
        }
        println("Sorted -------------")
        println(doc)

        update {
            people.removeIf { it.name == "William Berkley" }
        }
        println("William removed ----")
        println(doc)
    } finally {
        doc.dispose()
    }
}

fun Html.PersonComponent(person: Person) {
    div {
        span {
            text("Name: ${person.name}")
        }
        span {
            text("Age: ${person.age}")
        }
    }
}