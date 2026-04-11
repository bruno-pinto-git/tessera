package com.tessera

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TesseraApplication

fun main(args: Array<String>) {
    runApplication<TesseraApplication>(*args)
}
