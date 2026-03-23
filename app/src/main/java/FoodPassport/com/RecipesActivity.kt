package FoodPassport.com

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class RecipesActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var titleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipes)

        val country = intent.getStringExtra("country") ?: ""
        val countryEs = intent.getStringExtra("countryEs") ?: country
        titleText = findViewById(R.id.titleRecipes)
        listView = findViewById(R.id.listViewRecipes)

        titleText.text = "Recetas de $countryEs"

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadRecipes(country)
    }

    private fun loadRecipes(country: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.getRecipesByCountry(country)
                val meals = response.meals ?: emptyList()

                // Traduir els noms de les receptes
                val translatedMeals = meals.map { meal ->
                    try {
                        val result = RetrofitClient.translateApi.translate(meal.strMeal)
                        Pair(meal, result.responseData.translatedText)
                    } catch (e: Exception) {
                        Pair(meal, meal.strMeal)
                    }
                }

                withContext(Dispatchers.Main) {
                    val adapter = ArrayAdapter(
                        this@RecipesActivity,
                        android.R.layout.simple_list_item_1,
                        translatedMeals.map { it.second }.toMutableList()
                    )
                    listView.adapter = adapter

                    listView.setOnItemClickListener { _, _, position, _ ->
                        val meal = translatedMeals[position].first
                        val mealEs = translatedMeals[position].second
                        val intent = Intent(this@RecipesActivity, MealDetailActivity::class.java)
                        intent.putExtra("mealId", meal.idMeal)
                        intent.putExtra("mealNameEs", mealEs)
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RecipesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}