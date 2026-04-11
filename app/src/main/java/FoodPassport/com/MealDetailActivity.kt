package FoodPassport.com

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
    private var mealId = ""
    private var mealNameEs = ""
    private var mealThumb = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_meal_detail)

        mealId = intent.getStringExtra("mealId") ?: ""
        mealNameEs = intent.getStringExtra("mealNameEs") ?: ""
        mealThumb = intent.getStringExtra("mealThumb") ?: ""

        btnFavorite = findViewById(R.id.btnFavorite)

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
            ref.setValue(mapOf("name" to mealNameEs, "thumb" to mealThumb)).addOnSuccessListener {
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
                findViewById<TextView>(R.id.mealInstructions).text =
                    snapshot.getValue(String::class.java) ?: ""
            } else {
                translateAndSave(mealId, dbRef)
            }
        }.addOnFailureListener {
            translateAndSave(mealId, dbRef)
        }
    }

    private fun translateAndSave(mealId: String, dbRef: com.google.firebase.database.DatabaseReference) {
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
                dbRef.setValue(translatedInstructions.take(9000))

                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.mealInstructions).text = translatedInstructions
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MealDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}