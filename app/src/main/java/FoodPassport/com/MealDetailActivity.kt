package FoodPassport.com

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MealDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_detail)

        val mealId = intent.getStringExtra("mealId") ?: ""
        val mealNameEs = intent.getStringExtra("mealNameEs") ?: ""

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        if (mealNameEs.isNotEmpty()) {
            findViewById<TextView>(R.id.mealTitle).text = mealNameEs
        }

        loadMealDetail(mealId)
    }

    private fun loadMealDetail(mealId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.getMealDetail(mealId)
                val meal = response.meals?.firstOrNull()

                meal?.let {
                    val instructions = it.strInstructions ?: ""

                    // Dividir en trossos de 450 caràcters
                    val chunks = mutableListOf<String>()
                    var start = 0
                    while (start < instructions.length) {
                        val end = minOf(start + 450, instructions.length)
                        chunks.add(instructions.substring(start, end))
                        start = end
                    }

                    // Traduir cada tros
                    val translatedChunks = chunks.map { chunk ->
                        try {
                            RetrofitClient.translateApi.translate(chunk).responseData.translatedText
                        } catch (e: Exception) {
                            chunk
                        }
                    }

                    val translatedInstructions = translatedChunks.joinToString(" ")

                    withContext(Dispatchers.Main) {
                        findViewById<TextView>(R.id.mealInstructions).text = translatedInstructions
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MealDetailActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}