package FoodPassport.com

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*

class RecipesActivity : BaseActivity() {

    private lateinit var listView: ListView
    private lateinit var titleText: TextView

    private val database = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_recipes)

        val country = intent.getStringExtra("country") ?: ""
        val countryEs = intent.getStringExtra("countryEs") ?: country

        titleText = findViewById(R.id.titleRecipes)
        listView = findViewById(R.id.listViewRecipes)

        titleText.text = "Recetas de $countryEs"
        findViewById<TextView>(R.id.toolbarTitle).text = countryEs

        loadRecipes(country, countryEs)
    }

    // Afegit countryEs com a paràmetre per passar-lo a MealDetailActivity
    private fun loadRecipes(country: String, countryEs: String) {
        val dbRef = database.getReference("recipes/${country.replace(" ", "_")}")

        dbRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                fetchImagesAndShow(country, countryEs, snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    val name = child.getValue(String::class.java) ?: return@mapNotNull null
                    Pair(id, name)
                })
            } else {
                translateAndSave(country, countryEs, dbRef)
            }
        }.addOnFailureListener {
            translateAndSave(country, countryEs, dbRef)
        }
    }

    private fun fetchImagesAndShow(country: String, countryEs: String, savedMeals: List<Pair<String, String>>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.getRecipesByCountry(country)
                val apiMeals = response.meals ?: emptyList()
                val thumbMap = apiMeals.associate { it.idMeal to (it.strMealThumb ?: "") }
                val withImages = savedMeals.map { (id, name) ->
                    Triple(id, name, thumbMap[id] ?: "")
                }.sortedBy { it.second }

                withContext(Dispatchers.Main) { mostrarRecetas(withImages, countryEs) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mostrarRecetas(savedMeals.map { Triple(it.first, it.second, "") }, countryEs)
                }
            }
        }
    }

    private fun translateAndSave(country: String, countryEs: String, dbRef: com.google.firebase.database.DatabaseReference) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.getRecipesByCountry(country)
                val meals = response.meals ?: emptyList()

                val translated = meals.map { meal ->
                    try {
                        val result = RetrofitClient.translateApi.translate(meal.strMeal)
                        Triple(meal.idMeal, result.responseData.translatedText, meal.strMealThumb ?: "")
                    } catch (e: Exception) {
                        Triple(meal.idMeal, meal.strMeal, meal.strMealThumb ?: "")
                    }
                }

                val toSave = translated.associate { it.first to it.second }
                dbRef.setValue(toSave)

                val sorted = translated.sortedBy { it.second }
                withContext(Dispatchers.Main) { mostrarRecetas(sorted, countryEs) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RecipesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Afegit countryEs per passar-lo a MealDetailActivity
    private fun mostrarRecetas(meals: List<Triple<String, String, String>>, countryEs: String) {
        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.item_country_list,
            R.id.tvCountryItem,
            meals.map { it.second }.toMutableList()
        ) {}
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, MealDetailActivity::class.java)
            intent.putExtra("mealId", meals[position].first)
            intent.putExtra("mealNameEs", meals[position].second)
            intent.putExtra("mealThumb", meals[position].third)
            // Passa el nom del país en castellà per guardar-lo als favorits
            intent.putExtra("countryEs", countryEs)
            startActivity(intent)
        }
    }
}