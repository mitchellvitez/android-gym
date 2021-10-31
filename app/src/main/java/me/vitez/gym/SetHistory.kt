package me.vitez.gym

import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat

class SetHistory(
    val id: Int,
    val createdAt: Date,
    val exercise: String,
    val tier: Tier,
    val reps: Int,
    val repsOutOf: Int,
    val weight: Int,
) {}