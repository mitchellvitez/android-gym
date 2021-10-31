package me.vitez.gym

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.*


class WorkoutActivity : AppCompatActivity() {
    var setNumber = 1
    var reps = 5
    var exerciseNumber = 0
    var workoutData: ArrayList<Workout> = ArrayList()
    var timer: CountDownTimer = newTimer()
    var weight: Int = 45

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)
        val db = Database(applicationContext)
        workoutData = db.getNextWorkout()
        setupExercise(0)
        redraw()

        // reps +/- buttons
        val plusButton = findViewById<Button>(R.id.plusButton)
        plusButton.setOnClickListener {
            reps += 1
            redraw()
        }
        val minusButton = findViewById<Button>(R.id.minusButton)
        minusButton.setOnClickListener {
            if (reps > 0) {
                reps -= 1
            }
            redraw()
        }
    }

    fun setupExercise(i: Int) {
        val workout = workoutData[i]
        val db = Database(applicationContext)

        weight = db.getNextWeight(workout.exercise, workout.tier)!!

        val weightTextView = findViewById<TextView>(R.id.weight)
        weightTextView.text = "reps of " + weight.toString() + " lbs"

        val exerciseTextView = findViewById<TextView>(R.id.exerciseName)
        exerciseTextView.text = workout.exercise

        val workoutNameTextView = findViewById<TextView>(R.id.workoutName)
        workoutNameTextView.text = workout.name

        val setsOutOfTextView = findViewById<TextView>(R.id.setsOutOf)
        setsOutOfTextView.text = "of " + workout.sets.size.toString()

        setNumber = 1
        reps = workout.sets[setNumber - 1]
    }

    fun secondsToTimer(n: Long): String {
        if (n <= 0) {
            return "now"
        }
        else {
            return (n.div(60)).toString() + ":" + (n % 60).toString().padStart(2, '0')
        }
    }

    fun redraw() {
        val setNumberTextView = findViewById<TextView>(R.id.setNumber)
        setNumberTextView.text = "Set " + this.setNumber.toString()

        val repsTextView = findViewById<TextView>(R.id.repsNumber)
        repsTextView.text = reps.toString()

        val db = Database(applicationContext)
        val repsOutOf = workoutData[exerciseNumber].sets[setNumber - 1]

       // hide [-] button if reps is 0, and hide [+] button if reps is max
        val minusButton = findViewById<Button>(R.id.minusButton)
        if (reps == 0) {
            minusButton.visibility = View.INVISIBLE
        } else {
            minusButton.visibility = View.VISIBLE
        }
        val plusButton = findViewById<Button>(R.id.plusButton)
        if (reps == workoutData[exerciseNumber].sets[setNumber - 1]) {
            plusButton.visibility = View.INVISIBLE
        } else {
            plusButton.visibility = View.VISIBLE
        }

        val completeSetButton = findViewById<Button>(R.id.completeSet)
        if (setNumber >= workoutData[exerciseNumber].sets.size) {
            if (exerciseNumber >= workoutData.size - 1) {
                completeSetButton.text = "Finish Workout"
                completeSetButton.setOnClickListener {
                    timer.cancel()
                    db.setLastWorkout(workoutData[0].name)
                    db.addSetHistory(workoutData[exerciseNumber].exercise, workoutData[exerciseNumber].tier, reps, repsOutOf, weight)
                    finish()
                }
            } else {
                completeSetButton.text = "Next Exercise"
                completeSetButton.setOnClickListener {
                    db.addSetHistory(workoutData[exerciseNumber].exercise, workoutData[exerciseNumber].tier, reps, repsOutOf, weight)
                    timer.cancel()
                    val timerTextView = findViewById<TextView>(R.id.timer)
                    timerTextView.text = "when ready"
                    exerciseNumber += 1
                    setupExercise(exerciseNumber)
                    completeSetButton.text = "Complete Set"
                    redraw()
                }
            }
        } else {
            completeSetButton.setOnClickListener {
                db.addSetHistory(workoutData[exerciseNumber].exercise, workoutData[exerciseNumber].tier, reps, repsOutOf, weight)
                reps = workoutData[exerciseNumber].sets[setNumber]
                setNumber += 1
                startTimer()
                redraw()
            }
        }
    }

    fun startTimer() {
        timer.cancel()
        timer = newTimer()
        timer.start()
    }

    fun newTimer (): CountDownTimer {
        return object: CountDownTimer(1000 * 121, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val timerTextView = findViewById<TextView>(R.id.timer)
                timerTextView.text = secondsToTimer(millisUntilFinished.div(1000))
            }
            override fun onFinish() {
                val player: MediaPlayer = MediaPlayer.create(applicationContext, R.raw.timer)
                player.start()
                val timerTextView = findViewById<TextView>(R.id.timer)
                timerTextView.text = "now"
            }
        }
    }
}