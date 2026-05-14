package FoodPassport.com

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FavoritesActivity : BaseActivity() {

    private lateinit var listView: ListView
    private lateinit var emptyText: TextView
    private var items = listOf<FavoriteItem>()

    private val database = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")

    sealed class FavoriteItem {
        data class Header(val country: String) : FavoriteItem()
        data class Meal(val mealId: String, val name: String, val thumb: String, val country: String) : FavoriteItem()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_favorites)

        findViewById<TextView>(R.id.toolbarTitle).text = "Mis favoritos"

        listView = findViewById(R.id.listViewFavorites)
        emptyText = findViewById(R.id.emptyText)


        migrarFavoritsSensePais()
        loadFavorites()
    }


    private fun migrarFavoritsSensePais() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database.getReference("favorites/$uid").get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { child ->
                if (!child.hasChild("country")) {
                    child.ref.child("country").setValue("Sin país")
                }
            }
        }
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

            val meals = snapshot.children.mapNotNull { child ->
                val mealId = child.key ?: return@mapNotNull null
                val name = child.child("name").getValue(String::class.java) ?: return@mapNotNull null
                val thumb = child.child("thumb").getValue(String::class.java) ?: ""
                val country = child.child("country").getValue(String::class.java) ?: "Sin país"
                FavoriteItem.Meal(mealId, name, thumb, country)
            }


            val grouped = meals.groupBy { it.country }.toSortedMap()


            val flatList = mutableListOf<FavoriteItem>()
            grouped.forEach { (country, mealsOfCountry) ->
                flatList.add(FavoriteItem.Header(country))
                flatList.addAll(mealsOfCountry.sortedBy { it.name })
            }

            items = flatList
            emptyText.visibility = View.GONE
            listView.visibility = View.VISIBLE

            val adapter = object : BaseAdapter() {
                override fun getViewTypeCount() = 2
                override fun getItemViewType(position: Int) =
                    if (items[position] is FavoriteItem.Header) 0 else 1
                override fun getCount() = items.size
                override fun getItem(position: Int) = items[position]
                override fun getItemId(position: Int) = position.toLong()

                override fun isEnabled(position: Int) = items[position] is FavoriteItem.Meal

                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    return when (val item = items[position]) {


                        is FavoriteItem.Header -> {
                            val tv = TextView(this@FavoritesActivity)
                            tv.text = item.country
                            tv.setPadding(32, 20, 16, 8)
                            tv.textSize = 13f
                            tv.setTextColor(android.graphics.Color.parseColor("#9E0202"))
                            tv.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            tv.setBackgroundColor(android.graphics.Color.parseColor("#F5F5DC"))
                            tv
                        }


                        is FavoriteItem.Meal -> {
                            val view = convertView ?: layoutInflater.inflate(
                                R.layout.item_country_list, parent, false
                            )
                            view.findViewById<TextView>(R.id.tvCountryItem).text = item.name
                            view
                        }
                    }
                }
            }

            listView.adapter = adapter

            listView.setOnItemClickListener { _, _, position, _ ->
                val item = items[position]
                if (item is FavoriteItem.Meal) {
                    val intent = Intent(this, MealDetailActivity::class.java)
                    intent.putExtra("mealId", item.mealId)
                    intent.putExtra("mealNameEs", item.name)
                    intent.putExtra("mealThumb", item.thumb)
                    intent.putExtra("countryEs", item.country)
                    startActivity(intent)
                }
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