package me.vitez.gym

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

import android.content.Intent

class LauncherActivity : AppCompatActivity() {
    private var db: Database? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        db = Database(applicationContext)

        // TODO: remove this db reset, used for testing
        db!!.reset()

        val beginButton = findViewById<Button>(R.id.beginButton)
        beginButton.setOnClickListener {
            startActivity(Intent(this, WorkoutActivity::class.java))
        }

        val setupButton = findViewById<Button>(R.id.setupButton)
        setupButton.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        // TODO: create a settings page
        // TODO: create a stats page
        // TODO: create a way to add your own workout schedule
    }

    override fun onResume() {
        super.onResume()

        val workout = db!!.getNextWorkout()
        val workoutNameTextView = findViewById<View>(R.id.workoutName) as TextView
        workoutNameTextView.text = workout[0].name

        val layout: LinearLayout = findViewById(R.id.exerciseDescriptions)
        layout.removeAllViewsInLayout()
        workout.forEach {
            val textView = TextView(this@LauncherActivity)
            textView.text = exerciseDescription(it)
            textView.textSize = 20.0F
            textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            textView.setPadding(16,16,16,16)
            textView.setTextColor(resources.getColor(R.color.primary_extreme))
            layout.addView(textView)
        }
    }

    fun exerciseDescription(workout: Workout): String {
        val db = Database(applicationContext)
        val weight = db.getNextWeight(workout.exercise, workout.tier)
        return workout.exercise +
                    "  " +
                    setsToString(workout.sets) +
                    "  " +
                    weight +
                    " lbs"
    }

    // e.g. 3x8 or 5, 6, 7, 6, 5
    fun setsToString(sets: ArrayList<Int>): String {
        if (HashSet<Int>(sets).size == 1) {
            return sets.size.toString() + "×" + sets[0].toString()
        }
        return sets.joinToString(separator = ", "){ it.toString() }
    }
}