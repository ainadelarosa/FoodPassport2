package FoodPassport.com

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*

class MealDetailActivity : BaseActivity() {

    private val database = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")

    private var isFavorite = false
    private lateinit var btnFavorite: ImageButton
    private lateinit var btnShare: Button
    private var mealId = ""
    private var mealNameEs = ""
    private var mealThumb = ""
    private var countryEs = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_meal_detail)

        mealId = intent.getStringExtra("mealId") ?: ""
        mealNameEs = intent.getStringExtra("mealNameEs") ?: ""
        mealThumb = intent.getStringExtra("mealThumb") ?: ""
        countryEs = intent.getStringExtra("countryEs") ?: ""

        btnFavorite = findViewById(R.id.btnFavorite)
        btnShare = findViewById(R.id.btnShare)

        if (mealNameEs.isNotEmpty()) {
            findViewById<TextView>(R.id.mealTitle).text = mealNameEs
            findViewById<TextView>(R.id.toolbarTitle).text = mealNameEs
        }

        if (mealThumb.isNotEmpty()) {
            Glide.with(this)
                .load(mealThumb)
                .centerCrop()
                .into(findViewById(R.id.mealImage))
        }

        checkIfFavorite()
        loadMealDetail(mealId)

        btnFavorite.setOnClickListener { toggleFavorite() }
        btnShare.setOnClickListener { shareRecipe() }
    }

    // Compartir la recepta per WhatsApp o altres apps
    private fun shareRecipe() {
        val text = "🍽 $mealNameEs\n\nMira esta receta en FoodPassport!"

        // Intent que obre directament WhatsApp
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }

        try {
            // Intentar obrir WhatsApp directament
            startActivity(whatsappIntent)
        } catch (e: Exception) {
            // Si WhatsApp no està instal·lat, obrir el selector general d'apps
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(fallbackIntent, "Compartir receta"))
        }
    }

    private fun checkIfFavorite() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database.getReference("favorites/$uid/$mealId").get().addOnSuccessListener { snapshot ->
            isFavorite = snapshot.exists()
            updateStarIcon()
        }
    }

    private fun toggleFavorite() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }
        val ref = database.getReference("favorites/$uid/$mealId")

        if (isFavorite) {
            ref.removeValue().addOnSuccessListener {
                isFavorite = false
                updateStarIcon()
                Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
            }
        } else {
            ref.setValue(mapOf(
                "name" to mealNameEs,
                "thumb" to mealThumb,
                "country" to countryEs
            )).addOnSuccessListener {
                isFavorite = true
                updateStarIcon()
                Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStarIcon() {
        btnFavorite.setImageResource(
            if (isFavorite) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
    }

    private fun loadMealDetail(mealId: String) {
        val dbRef = database.getReference("instructions/$mealId")
        dbRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val text = snapshot.getValue(String::class.java) ?: ""
                showInstructions(text)
            } else {
                translateAndSave(mealId, dbRef)
            }
        }.addOnFailureListener {
            translateAndSave(mealId, dbRef)
        }
    }

    private fun translateAndSave(
        mealId: String,
        dbRef: com.google.firebase.database.DatabaseReference
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.getMealDetail(mealId)
                val meal = response.meals?.firstOrNull() ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MealDetailActivity, "Receta no encontrada", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val instructions = meal.strInstructions
                if (instructions.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MealDetailActivity, "Instrucciones vacías", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val chunks = mutableListOf<String>()
                var start = 0
                while (start < instructions.length) {
                    val end = minOf(start + 450, instructions.length)
                    chunks.add(instructions.substring(start, end))
                    start = end
                }

                val translatedChunks = chunks.map { chunk ->
                    try {
                        RetrofitClient.translateApi.translate(chunk).responseData.translatedText
                    } catch (e: Exception) { chunk }
                }

                val translatedInstructions = translatedChunks.joinToString(" ")
                val ingredients = buildIngredientsList(meal)

                val fullText = if (ingredients.isNotEmpty()) {
                    "INGREDIENTES:\n$ingredients\n\nPREPARACIÓN:\n$translatedInstructions"
                } else {
                    translatedInstructions
                }

                dbRef.setValue(fullText.take(9000))

                withContext(Dispatchers.Main) {
                    showInstructions(fullText)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MealDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun buildIngredientsList(meal: Meal): String {
        val ingredients = listOf(
            meal.strIngredient1, meal.strIngredient2, meal.strIngredient3,
            meal.strIngredient4, meal.strIngredient5, meal.strIngredient6,
            meal.strIngredient7, meal.strIngredient8, meal.strIngredient9,
            meal.strIngredient10, meal.strIngredient11, meal.strIngredient12,
            meal.strIngredient13, meal.strIngredient14, meal.strIngredient15,
            meal.strIngredient16, meal.strIngredient17, meal.strIngredient18,
            meal.strIngredient19, meal.strIngredient20
        )
        val measures = listOf(
            meal.strMeasure1, meal.strMeasure2, meal.strMeasure3,
            meal.strMeasure4, meal.strMeasure5, meal.strMeasure6,
            meal.strMeasure7, meal.strMeasure8, meal.strMeasure9,
            meal.strMeasure10, meal.strMeasure11, meal.strMeasure12,
            meal.strMeasure13, meal.strMeasure14, meal.strMeasure15,
            meal.strMeasure16, meal.strMeasure17, meal.strMeasure18,
            meal.strMeasure19, meal.strMeasure20
        )

        return ingredients.indices
            .filter { !ingredients[it].isNullOrBlank() }
            .joinToString("\n") { i ->
                val measure = measures.getOrNull(i)?.trim() ?: ""
                val ingredient = ingredients[i]?.trim() ?: ""
                if (measure.isNotEmpty()) "• $measure $ingredient"
                else "• $ingredient"
            }
    }

    private fun showInstructions(fullText: String) {
        if (fullText.contains("INGREDIENTES:") && fullText.contains("PREPARACIÓN:")) {
            val parts = fullText.split("PREPARACIÓN:")
            val ingredientsPart = parts[0].replace("INGREDIENTES:", "").trim()
            val prepPart = parts[1].trim()

            findViewById<TextView>(R.id.mealIngredients).text = ingredientsPart
            findViewById<TextView>(R.id.mealInstructions).text = prepPart

            findViewById<android.widget.LinearLayout>(R.id.sectionIngredients).visibility =
                android.view.View.VISIBLE
        } else {
            findViewById<android.widget.LinearLayout>(R.id.sectionIngredients).visibility =
                android.view.View.GONE
            findViewById<TextView>(R.id.mealInstructions).text = fullText
        }
    }
}