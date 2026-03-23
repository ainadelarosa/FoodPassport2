package FoodPassport.com

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class CountriesActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var searchBar: EditText
    private var allCountries = listOf<Pair<String, String>>()
    private var filteredCountries = listOf<Pair<String, String>>()
    private var adapter: ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countries)

        listView = findViewById(R.id.listViewCountries)
        searchBar = findViewById(R.id.searchBarCountries)

        // Inicialitzar adapter buit per evitar errors
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        loadCountries()

        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (adapter == null) return
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

                allCountries = translated
                filteredCountries = translated

                withContext(Dispatchers.Main) {
                    adapter?.clear()
                    adapter?.addAll(translated.map { it.second })
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