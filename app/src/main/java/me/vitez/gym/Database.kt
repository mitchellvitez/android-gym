package me.vitez.gym

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.getIntOrNull
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime.now
import kotlin.math.roundToInt

class Database(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        this.create(db)
        this.populate(db)
    }

    fun create(db: SQLiteDatabase) {
        db.execSQL(
            "create table if not exists workouts (id integer primary key, name text, tier text, exercise text, sets text)"
        )
        db.execSQL(
            "create table if not exists set_history (id integer primary key, created_at timestamp, tier text, exercise text, reps int, reps_out_of int, weight int)"
        )
        db.execSQL(
            "create table if not exists storage (id text primary key, value text)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, p1: Int, p2: Int) {
        // TODO: support db upgrades
    }

    fun reset() {
        writableDatabase.execSQL("drop table if exists workouts ")
        writableDatabase.execSQL("drop table if exists set_history ")
        writableDatabase.execSQL("drop table if exists storage ")
        this.create(writableDatabase)
        this.populate(writableDatabase)
    }

    fun populate(db: SQLiteDatabase) {
        db.execSQL("""
            insert into workouts (id, name, tier, exercise, sets) values
                (1, 'Deadlift & OHP', 'Maxing', 'Deadlift', '3,3,3')
              , (2, 'Deadlift & OHP', 'Normal', 'Overhead Press', '8,8,8')
              , (3, 'Bench & Squat', 'Maxing', 'Bench', '3,3,3')
              , (4, 'Bench & Squat', 'Normal', 'Squat', '8,8,8')
              , (5, 'OHP & Curl', 'Maxing', 'Overhead Press', '3,3,3')
              , (6, 'OHP & Curl', 'Normal', 'Barbell Curl', '8,8,8')
              , (7, 'OHP & Curl', 'Additional', 'Pullup', '3')
              , (8, 'Squat & Bench', 'Maxing', 'Squat', '3,3,3')
              , (9, 'Squat & Bench', 'Normal', 'Bench', '8,8,8')
        """.trimIndent())

        // TODO: remove this test data
//        db.execSQL("""
//            insert into set_history (id, created_at, exercise, tier, reps, reps_out_of, weight) values
//              (2134, '2021-02-10 9:02:15', 'Bench', 'Maxing', 3, 3, 240)
//            , (2135, '2021-02-10 9:03:48', 'Bench', 'Maxing', 3, 3, 245)
//            , (2136, '2021-02-10 9:06:37', 'Bench', 'Normal', 3, 3, 250)
//            , (2137, '2021-02-10 9:07:22', 'Squat', 'Maxing', 8, 8, 220)
//            , (2138, '2021-02-10 9:09:52', 'Squat', 'Normal', 8, 8, 225)
//            , (2139, '2021-02-10 9:10:18', 'Squat', 'Normal', 8, 8, 230)
//            , (2140, '2021-02-10 9:06:37', 'Bench', 'Maxing', 3, 3, 195)
//            , (2141, '2021-02-10 9:07:22', 'Squat', 'Normal', 8, 8, 225)
//        """.trimIndent())

        db.execSQL("""
            insert into storage values ('last workout', 'Deadlift & OHP')
        """.trimIndent())
    }

    fun addWorkout(name : String, tier : Tier, exercise: String, sets: Array<Int>){
        val values = ContentValues()
        values.put("name", name)
        values.put("tier", tier.toString())
        values.put("exercise", exercise)
        values.put("sets", sets.joinToString(separator = ","){ it.toString() })

        writableDatabase.insert("workouts", null, values)
    }

    fun addSetHistory(exercise: String, tier : Tier, reps: Int, repsOutOf: Int, weight: Int){
        Log.d(null, "added ${exercise} ${tier} ${reps} ${weight} to database")
        val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val timestamp = Timestamp(System.currentTimeMillis())

        val values = ContentValues()
        values.put("created_at", sdf.format(timestamp))
        values.put("exercise", exercise)
        values.put("tier", tier.toString())
        values.put("reps", reps)
        values.put("reps_out_of", repsOutOf)
        values.put("weight", weight)

        writableDatabase.insert("set_history", null, values)
    }

    fun setLastWorkout(lastWorkout: String) {
        writableDatabase.execSQL(
            "update storage set value = ? where id = 'last workout'", arrayOf(lastWorkout)
        )
    }

    fun getNextWorkoutName(): String {
        val cursor = readableDatabase.rawQuery("select value from storage where id = 'last workout'", null)

        cursor.moveToFirst()
        val lastWorkoutName = cursor.getString(cursor.getColumnIndexOrThrow("value"))
        cursor.close()

        val cursor2 = readableDatabase.rawQuery("""
            select name
            from workouts
            WHERE id > (select max(id) from workouts where name = ?)
            order by id
            limit 1""", arrayOf(lastWorkoutName))
        return if (cursor2.moveToFirst()) {
            val nextWorkoutName = cursor2.getString(cursor2.getColumnIndexOrThrow("name"))
            cursor2.close()
            nextWorkoutName
        } else {
            cursor2.close()
            val cursor3 = readableDatabase.rawQuery("select name from workouts order by id asc limit 1", null)
            cursor3.moveToFirst()
            val nextWorkoutName = cursor3.getString(cursor3.getColumnIndexOrThrow("name"))
            cursor3.close()
            nextWorkoutName
        }
    }

    fun totalSetsForExercise(exercise: String): Int {
        val cursor = readableDatabase.rawQuery("select count(id) from set_history where exercise = ?", arrayOf(exercise))
        val ret = cursor.getIntOrNull(0)
        cursor.close()

        if (ret != null) {
            return ret
        }
        return 0
    }

    fun getHistoryForExerciseTier(exercise: String, tier: Tier): ArrayList<SetHistory> {
        val cursor = readableDatabase.rawQuery("select * from set_history where exercise = ? and tier = ? order by created_at desc", arrayOf(exercise, tier.toString()))
        val arr : ArrayList<SetHistory> = ArrayList()
        if (cursor.moveToFirst()) {
            do {
                arr.add(SetHistory(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    SimpleDateFormat("yyyy-MM-DD HH:mm:ss").parse(cursor.getString(cursor.getColumnIndexOrThrow("created_at"))) as Date,
                    cursor.getString(cursor.getColumnIndexOrThrow("exercise")),
                    Tier.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("tier"))),
                    cursor.getInt(cursor.getColumnIndexOrThrow("reps")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("reps_out_of")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("weight"))
                ))
            } while(cursor.moveToNext())
        }
        cursor.close()
        return arr
    }

    fun getNextWeight(exercise: String, tier: Tier): Int {
        val cursor = readableDatabase.rawQuery("select * from set_history where exercise = ? and tier = ? order by created_at desc limit 1", arrayOf(exercise, tier.toString()))
        if (cursor.moveToFirst()) {
            val weight = cursor.getInt(cursor.getColumnIndexOrThrow("weight"))

            val failCursor = readableDatabase.rawQuery("""
                select coalesce(sum(failures), 0) as f from (
                    select sum(reps) < sum(reps_out_of) as failures
                    from set_history sh
                    where exercise = ?
                    and tier = ?
                    group by substr(created_at, 0, 11)
                    order by created_at desc
                    limit 2
                )
            """.trimIndent(), arrayOf(exercise, tier.toString()))
            failCursor.moveToFirst()
            val failureNumber = failCursor.getInt(failCursor.getColumnIndexOrThrow("f"))
            failCursor.close()

            Log.d("failure number", failureNumber.toString())

            if (failureNumber == 2) {
                var x: Int = (weight - (weight * 0.10)).roundToInt()
                while (x % 5 != 0) {
                    x -= 1
                }
                return maxOf(x, 45)
            } else if (failureNumber == 1) {
                return weight
            }
            return weight + 5
        }
        cursor.close()
        return 45
    }

    // TODO: stats/history page
    fun getNRepMax(exercise: String, reps: Int): Int? {
        val cursor = readableDatabase.rawQuery("select maximum(weight) from set_history where exercise = ? and reps = ? order by created_at desc", arrayOf(exercise, reps.toString()))
        val ret = cursor.getIntOrNull(0)
        cursor.close()
        return ret
    }

    fun getTotalReps(exercise: String): Int {
        val cursor = readableDatabase.rawQuery("select sum(reps) from set_history where exercise = ?", arrayOf(exercise))
        val ret = cursor.getIntOrNull(0)
        cursor.close()
        if (ret != null) {
            return ret
        }
        return 0
    }

    fun getNextWorkout(): ArrayList<Workout> {
        val cursor = readableDatabase.rawQuery("select * from workouts where name = ?", arrayOf(getNextWorkoutName()))
        val arr : ArrayList<Workout> = ArrayList()
        if (cursor.moveToFirst()) {
            do {
                arr.add(Workout(
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    Tier.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("tier"))),
                    cursor.getString(cursor.getColumnIndexOrThrow("exercise")),
                    ArrayList(cursor.getString(cursor.getColumnIndexOrThrow("sets")).split(",").map { it.toInt() })
                ))
            } while(cursor.moveToNext())
        }
        cursor.close()
        return arr
    }

    companion object{
        private val DATABASE_NAME = "VITEZ_GYM_DB"
        private val DATABASE_VERSION = 1
    }
}