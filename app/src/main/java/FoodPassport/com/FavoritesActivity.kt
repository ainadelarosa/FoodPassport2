package FoodPassport.com

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FavoritesActivity : BaseActivity() {

    private lateinit var listView: ListView
    private lateinit var emptyText: TextView
    private var favorites = listOf<Triple<String, String, String>>()

    private val database = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_favorites)

        findViewById<TextView>(R.id.toolbarTitle).text = "Mis favoritos"

        listView = findViewById(R.id.listViewFavorites)
        emptyText = findViewById(R.id.emptyText)

        loadFavorites()
    }

    private fun loadFavorites() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database.getReference("favorites/$uid").get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                listView.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                return@addOnSuccessListener
            }

            val list = snapshot.children.mapNotNull { child ->
                val mealId = child.key ?: return@mapNotNull null
                val name = child.child("name").getValue(String::class.java) ?: return@mapNotNull null
                val thumb = child.child("thumb").getValue(String::class.java) ?: ""
                Triple(mealId, name, thumb)
            }.sortedBy { it.second }

            favorites = list
            emptyText.visibility = View.GONE
            listView.visibility = View.VISIBLE

            listView.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                list.map { it.second }.toMutableList()
            )

            listView.setOnItemClickListener { _, _, position, _ ->
                val meal = favorites[position]
                val intent = Intent(this, MealDetailActivity::class.java)
                intent.putExtra("mealId", meal.first)
                intent.putExtra("mealNameEs", meal.second)
                intent.putExtra("mealThumb", meal.third)
                startActivity(intent)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }
}