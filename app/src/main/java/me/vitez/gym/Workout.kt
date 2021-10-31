package me.vitez.gym

import java.sql.Date

class Workout(
    val name: String,
    val tier: Tier,
    val exercise: String,
    val sets: ArrayList<Int>,
) {}