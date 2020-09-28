package com.example.themontyhallgame

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.ColorFilter
import android.os.Bundle
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.random.Random

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    enum class GameState(val value: Int) {
        BEGIN(0),
        SELECT_FIRST_DOOR(1),
        ELIMINATE_DOOR(2),
        ASK_SWITCH_DOOR(3),
        SHOW_FINAL_SELECTION(4),
        SHOW_RESULTS(5),
        RESTART(6);

        companion object {
            fun fromInt(value: Int) = GameState.values().first { it.value == value }
        }
    }

    private var gameState: GameState = GameState.BEGIN

    private lateinit var doors: Array<ImageView>
    private lateinit var crosses: Array<ImageView>
    private lateinit var coins: Array<ImageView>
    private lateinit var donkeys: Array<ImageView>

    private lateinit var buttons: Array<Button>

    private lateinit var continueButton: Button

    private lateinit var instructionText: TextView

    private var doorIndex: Int = -1
    private var selectedDoorIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        doors = arrayOf(
            findViewById(R.id.image_door1),
            findViewById(R.id.image_door2),
            findViewById(R.id.image_door3)
        )
        crosses = arrayOf(
            findViewById(R.id.image_cross1),
            findViewById(R.id.image_cross2),
            findViewById(R.id.image_cross3)
        )
        coins = arrayOf(
            findViewById(R.id.image_coin1),
            findViewById(R.id.image_coin2),
            findViewById(R.id.image_coin3)
        )
        donkeys = arrayOf(
            findViewById(R.id.image_donkey1),
            findViewById(R.id.image_donkey2),
            findViewById(R.id.image_donkey3)
        )
        buttons = arrayOf(
            findViewById(R.id.button_door1),
            findViewById(R.id.button_door2),
            findViewById(R.id.button_door3)
        )

        for (i in buttons.indices) {
            buttons[i].setOnClickListener { onButtonClicked(i) }
        }

        instructionText = findViewById(R.id.textView_instructions)

        continueButton = findViewById(R.id.button_continue)
        continueButton.setOnClickListener { incrementGameState(); }

        Log.d(TAG, "onCreate called")

        initGame()
    }

    private fun incrementGameState() {
        val stateCount = GameState.RESTART.value + 1
        gameState = GameState.fromInt((gameState.value + 1) % stateCount)

        Log.d(TAG, "Incremented gameState to $gameState")

        when (gameState) {
            GameState.BEGIN -> {
                initGame()
            }
            GameState.SELECT_FIRST_DOOR -> {
            }
            GameState.ELIMINATE_DOOR -> {

                var index = -1;

                when (doorIndex) {
                    0 -> {
                        index = when (selectedDoorIndex) {
                            1 -> 2
                            2 -> 1
                            else -> Random.nextInt(1, 3)
                        }
                    }
                    1 -> {
                        index = when (selectedDoorIndex) {
                            0 -> 2
                            2 -> 0
                            else -> Random.nextInt(0, 2) * 2
                        }
                    }
                    2 -> {
                        index = when (selectedDoorIndex) {
                            0 -> 1
                            1 -> 0
                            else -> Random.nextInt(0, 2)
                        }
                    }
                }

                eliminateDoor(index)
                setButtonActive(continueButton, false)
            }
            GameState.ASK_SWITCH_DOOR -> {

            }
            GameState.SHOW_FINAL_SELECTION -> {
                for (button in buttons) {
                    setButtonActive(button, false)
                }

                for (i in doors.indices) {
                    val col = if (i == selectedDoorIndex) {
                        Color.BLACK
                    } else {
                        Color.LTGRAY
                    }

                    doors[i].setColorFilter(col)
                    crosses[i].alpha = 0f
                }

                instructionText.text = getString(R.string.showFinalSelection).replace("selectedDoorIndex", (selectedDoorIndex + 1).toString())
                setButtonActive(continueButton, true)
            }
            GameState.SHOW_RESULTS -> {
                setButtonActive(continueButton, false)
                showResults()
            }
            GameState.RESTART -> {
                setButtonActive(continueButton, true)
                continueButton.text = getString(R.string.restartText)
                if (selectedDoorIndex == doorIndex) {
                    instructionText.text = getString(R.string.win)
                } else {
                    instructionText.text = getString(R.string.lose)
                }
            }
        }
    }

    private fun initGame() {
        gameState = GameState.BEGIN

        continueButton.text = getString(R.string.continueText)

        for (door in doors) {
            door.setImageResource(R.drawable.ic_closed_filled_rectangular_door)
            door.setColorFilter(Color.BLACK)
        }

        for (cross in crosses) {
            cross.alpha = 0.0f
        }

        for (coin in coins) {
            coin.alpha = 0.0f
        }

        for (donkey in donkeys) {
            donkey.alpha = 0.0f
        }

        for (button in buttons) {
            setButtonActive(button, true)
        }

        setButtonActive(continueButton, false)

        doorIndex = Random.nextInt(0, 3)
        Log.d(TAG, "The door index is $doorIndex")
        selectedDoorIndex = -1

        instructionText.text = getString(R.string.begin)

        incrementGameState()
    }

    private fun onButtonClicked(index: Int) {
        Log.d(TAG, "Button clicked: $index");

        if (gameState == GameState.SELECT_FIRST_DOOR) {
            selectedDoorIndex = index
            instructionText.text = getString(R.string.selectFirstDoor).replace("selectedDoorIndex", (selectedDoorIndex + 1).toString())
            setButtonActive(continueButton, true)
            for (button in buttons) {
                setButtonActive(button, false)
            }
        } else if (gameState == GameState.ASK_SWITCH_DOOR) {
            selectedDoorIndex = index
            incrementGameState()
        }
    }

    private fun eliminateDoor(index: Int) {



        for (i in 0..2) {
            setButtonActive(buttons[i], i != index)
        }

        doors[index].setColorFilter(Color.LTGRAY);

        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.addUpdateListener {
            val value = it.animatedValue as Float
            crosses[index].alpha = value

            if (value == 1f) {
                instructionText.text = getString(R.string.eliminateDoor).replace("index", (index + 1).toString())
                incrementGameState();
            }
        }

        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.duration = 500
        valueAnimator.startDelay = 500
        valueAnimator.start()
    }

    private fun showResults() {
        for (door in doors) {
            door.setImageResource(R.drawable.ic_opened_filled_door)
        }

        for (cross in crosses) {
            cross.alpha = 0f
        }

        val valueAnimator = ValueAnimator.ofFloat(0f, 2f)
        valueAnimator.duration = 2000;
        valueAnimator.addUpdateListener {
            val value = it.animatedValue as Float
            for (i in doors.indices) {
                val selected = i == selectedDoorIndex;
                val alpha = if (selected) {
                    value
                } else {
                    value - 1f
                }

                alpha.coerceIn(0f, 1f); // clamps value between 0 and 1

                val prize = if (i == doorIndex) {
                    coins[i]
                } else {
                    donkeys[i]
                }

                prize.alpha = alpha;
            }

            if (value == 2f)
                incrementGameState();

        }
        valueAnimator.start()


//        for (i in doors.indices) {
//            val prize = if (i == doorIndex) {
//                coins[i]
//            } else {
//                donkeys[i]
//            }
//
//            val showSelectedAnimator = ValueAnimator.ofFloat(0f, 1f)
//            showSelectedAnimator.addUpdateListener {
//                prize.alpha = it.animatedValue as Float;
//            }
//
//            showSelectedAnimator.interpolator = LinearInterpolator()
//            showSelectedAnimator.duration = 600
//            showSelectedAnimator.startDelay = if (i == selectedDoorIndex) {
//                0
//            } else {
//                1000
//            }
//            showSelectedAnimator.start()
//        }

//        incrementGameState();
    }

    private fun setButtonActive(button: Button, value: Boolean) {
        button.isEnabled = value
        button.isClickable = value
    }
}