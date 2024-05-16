package com.jeongum.springlogging

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringLoggingApplication

fun main(args: Array<String>) {
    runApplication<SpringLoggingApplication>(*args)
}
