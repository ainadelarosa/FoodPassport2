package FoodPassport.com

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*

class CountriesActivity : BaseActivity() {

    private lateinit var listView: ListView
    private lateinit var searchBar: EditText
    private var allCountries = listOf<Pair<String, String>>()
    private var filteredCountries = listOf<Pair<String, String>>()
    private var adapter: ArrayAdapter<String>? = null

    private val dbRef = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")
        .getReference("countries")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_countries)

        findViewById<TextView>(R.id.toolbarTitle).text = "Recetas por países"

        listView = findViewById(R.id.listViewCountries)
        searchBar = findViewById(R.id.searchBarCountries)

        adapter = object : ArrayAdapter<String>(
            this,
            R.layout.item_country_list,
            R.id.tvCountryItem,
            mutableListOf()
        ) {}
        listView.adapter = adapter

        loadCountries()

        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                filteredCountries = allCountries.filter {
                    it.second.lowercase().contains(query)
                }
                adapter?.clear()
                adapter?.addAll(filteredCountries.map { it.second })
                adapter?.notifyDataSetChanged()
            }
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val country = filteredCountries[position]
            val intent = Intent(this, RecipesActivity::class.java)
            intent.putExtra("country", country.first)
            intent.putExtra("countryEs", country.second)
            startActivity(intent)
        }
    }

    private fun loadCountries() {
        dbRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val countries = snapshot.children.mapNotNull { child ->
                    val original = child.key?.replace("_", " ") ?: return@mapNotNull null
                    val translated = child.getValue(String::class.java) ?: return@mapNotNull null
                    Pair(original, translated)
                }.sortedBy { it.second }

                allCountries = countries
                filteredCountries = countries

                runOnUiThread {
                    adapter?.clear()
                    adapter?.addAll(countries.map { it.second })
                    adapter?.notifyDataSetChanged()
                }
            } else {
                translateAndSave()
            }
        }.addOnFailureListener {
            translateAndSave()
        }
    }

    private fun translateAndSave() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.getCountries()
                val countries = response.meals?.map { it.strArea } ?: emptyList()

                val translated = countries.map { country ->
                    try {
                        val result = RetrofitClient.translateApi.translate(country)
                        Pair(country, result.responseData.translatedText)
                    } catch (e: Exception) {
                        Pair(country, country)
                    }
                }

                val toSave = translated.associate {
                    it.first.replace(" ", "_") to it.second
                }
                dbRef.setValue(toSave)

                val sorted = translated.sortedBy { it.second }
                allCountries = sorted
                filteredCountries = sorted

                withContext(Dispatchers.Main) {
                    adapter?.clear()
                    adapter?.addAll(sorted.map { it.second })
                    adapter?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CountriesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}