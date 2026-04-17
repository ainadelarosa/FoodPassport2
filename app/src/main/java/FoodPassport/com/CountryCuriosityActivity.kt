package FoodPassport.com

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*

class CountryCuriosityActivity : BaseActivity() {

    private lateinit var searchBar: EditText
    private lateinit var searchBtn: Button
    private lateinit var loadingText: TextView
    private lateinit var cardContainer: LinearLayout
    private lateinit var scrollCardContainer: ScrollView
    private lateinit var flagImage: ImageView
    private lateinit var tvCountryName: TextView
    private lateinit var tvOfficialName: TextView
    private lateinit var tvCapital: TextView
    private lateinit var tvRegion: TextView
    private lateinit var tvPopulation: TextView
    private lateinit var tvArea: TextView
    private lateinit var tvLanguages: TextView
    private lateinit var tvCurrency: TextView
    private lateinit var tvMeals: TextView
    private lateinit var listViewCountries: ListView

    private val db = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")

    private val countryTranslations = mapOf(
        "afganistán" to "Afghanistan", "albania" to "Albania", "alemania" to "Germany",
        "andorra" to "Andorra", "angola" to "Angola", "arabia saudita" to "Saudi Arabia",
        "argelia" to "Algeria", "argentina" to "Argentina", "armenia" to "Armenia",
        "australia" to "Australia", "austria" to "Austria", "azerbaiyán" to "Azerbaijan",
        "bahamas" to "Bahamas", "bahrein" to "Bahrain", "bangladés" to "Bangladesh",
        "barbados" to "Barbados", "bélgica" to "Belgium", "belice" to "Belize",
        "benín" to "Benin", "bielorrusia" to "Belarus", "bolivia" to "Bolivia",
        "bosnia y herzegovina" to "Bosnia and Herzegovina", "botsuana" to "Botswana",
        "brasil" to "Brazil", "brunéi" to "Brunei", "bulgaria" to "Bulgaria",
        "burkina faso" to "Burkina Faso", "burundi" to "Burundi", "bután" to "Bhutan",
        "cabo verde" to "Cape Verde", "camboya" to "Cambodia", "camerún" to "Cameroon",
        "canadá" to "Canada", "catar" to "Qatar", "chad" to "Chad",
        "chile" to "Chile", "china" to "China", "chipre" to "Cyprus",
        "colombia" to "Colombia", "comoras" to "Comoros", "corea del norte" to "North Korea",
        "corea del sur" to "South Korea", "costa de marfil" to "Ivory Coast",
        "costa rica" to "Costa Rica", "croacia" to "Croatia", "cuba" to "Cuba",
        "dinamarca" to "Denmark", "dominica" to "Dominica", "ecuador" to "Ecuador",
        "egipto" to "Egypt", "el salvador" to "El Salvador",
        "emiratos árabes unidos" to "United Arab Emirates", "eritrea" to "Eritrea",
        "eslovaquia" to "Slovakia", "eslovenia" to "Slovenia", "españa" to "Spain",
        "estados unidos" to "United States", "estonia" to "Estonia", "etiopía" to "Ethiopia",
        "filipinas" to "Philippines", "finlandia" to "Finland", "fiyi" to "Fiji",
        "francia" to "France", "gabón" to "Gabon", "gambia" to "Gambia",
        "georgia" to "Georgia", "ghana" to "Ghana", "granada" to "Grenada",
        "grecia" to "Greece", "guatemala" to "Guatemala", "guinea" to "Guinea",
        "guinea ecuatorial" to "Equatorial Guinea", "guinea-bisáu" to "Guinea-Bissau",
        "guyana" to "Guyana", "haití" to "Haiti", "honduras" to "Honduras",
        "hungría" to "Hungary", "india" to "India", "indonesia" to "Indonesia",
        "irak" to "Iraq", "irán" to "Iran", "irlanda" to "Ireland",
        "islandia" to "Iceland", "islas marshall" to "Marshall Islands",
        "islas salomón" to "Solomon Islands", "israel" to "Israel", "italia" to "Italy",
        "jamaica" to "Jamaica", "japón" to "Japan", "jordania" to "Jordan",
        "kazajistán" to "Kazakhstan", "kenia" to "Kenya", "kirguistán" to "Kyrgyzstan",
        "kiribati" to "Kiribati", "kuwait" to "Kuwait", "laos" to "Laos",
        "lesoto" to "Lesotho", "letonia" to "Latvia", "líbano" to "Lebanon",
        "liberia" to "Liberia", "libia" to "Libya", "liechtenstein" to "Liechtenstein",
        "lituania" to "Lithuania", "luxemburgo" to "Luxembourg", "madagascar" to "Madagascar",
        "malasia" to "Malaysia", "malaui" to "Malawi", "maldivas" to "Maldives",
        "malí" to "Mali", "malta" to "Malta", "marruecos" to "Morocco",
        "mauricio" to "Mauritius", "mauritania" to "Mauritania", "méxico" to "Mexico",
        "micronesia" to "Micronesia", "moldavia" to "Moldova", "mónaco" to "Monaco",
        "mongolia" to "Mongolia", "montenegro" to "Montenegro", "mozambique" to "Mozambique",
        "namibia" to "Namibia", "nauru" to "Nauru", "nepal" to "Nepal",
        "nicaragua" to "Nicaragua", "níger" to "Niger", "nigeria" to "Nigeria",
        "noruega" to "Norway", "nueva zelanda" to "New Zealand", "omán" to "Oman",
        "países bajos" to "Netherlands", "pakistán" to "Pakistan", "palaos" to "Palau",
        "panamá" to "Panama", "papúa nueva guinea" to "Papua New Guinea",
        "paraguay" to "Paraguay", "perú" to "Peru", "polonia" to "Poland",
        "portugal" to "Portugal", "reino unido" to "United Kingdom",
        "república centroafricana" to "Central African Republic",
        "república checa" to "Czech Republic", "república del congo" to "Republic of the Congo",
        "república democrática del congo" to "Democratic Republic of the Congo",
        "república dominicana" to "Dominican Republic", "ruanda" to "Rwanda",
        "rumania" to "Romania", "rusia" to "Russia", "samoa" to "Samoa",
        "san cristóbal y nieves" to "Saint Kitts and Nevis", "san marino" to "San Marino",
        "san vicente y las granadinas" to "Saint Vincent and the Grenadines",
        "santa lucía" to "Saint Lucia", "santo tomé y príncipe" to "São Tomé and Príncipe",
        "senegal" to "Senegal", "serbia" to "Serbia", "seychelles" to "Seychelles",
        "sierra leona" to "Sierra Leone", "singapur" to "Singapore", "siria" to "Syria",
        "somalia" to "Somalia", "sri lanka" to "Sri Lanka", "suazilandia" to "Eswatini",
        "sudáfrica" to "South Africa", "sudán" to "Sudan", "sudán del sur" to "South Sudan",
        "suecia" to "Sweden", "suiza" to "Switzerland", "surinam" to "Suriname",
        "tailandia" to "Thailand", "tanzania" to "Tanzania", "tayikistán" to "Tajikistan",
        "timor oriental" to "Timor-Leste", "togo" to "Togo", "tonga" to "Tonga",
        "trinidad y tobago" to "Trinidad and Tobago", "túnez" to "Tunisia",
        "turkmenistán" to "Turkmenistan", "turquía" to "Turkey", "tuvalu" to "Tuvalu",
        "ucrania" to "Ukraine", "uganda" to "Uganda", "uruguay" to "Uruguay",
        "uzbekistán" to "Uzbekistan", "vanuatu" to "Vanuatu", "venezuela" to "Venezuela",
        "vietnam" to "Vietnam", "yemen" to "Yemen", "yibuti" to "Djibouti",
        "zambia" to "Zambia", "zimbabue" to "Zimbabwe",
        "antigua y barbuda" to "Antigua and Barbuda"
    )

    private var filteredCountries = countryTranslations.keys.sorted().toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_country_curiosity)

        findViewById<TextView>(R.id.toolbarTitle).text = "Curiosidades"

        searchBar = findViewById(R.id.searchCuriosity)
        searchBtn = findViewById(R.id.btnSearchCuriosity)
        loadingText = findViewById(R.id.loadingCuriosity)
        scrollCardContainer = findViewById(R.id.scrollCardContainer)
        cardContainer = findViewById(R.id.cardContainer)
        flagImage = findViewById(R.id.flagImage)
        tvCountryName = findViewById(R.id.tvCountryName)
        tvOfficialName = findViewById(R.id.tvOfficialName)
        tvCapital = findViewById(R.id.tvCapital)
        tvRegion = findViewById(R.id.tvRegion)
        tvPopulation = findViewById(R.id.tvPopulation)
        tvArea = findViewById(R.id.tvArea)
        tvLanguages = findViewById(R.id.tvLanguages)
        tvCurrency = findViewById(R.id.tvCurrency)
        tvMeals = findViewById(R.id.tvMeals)
        listViewCountries = findViewById(R.id.listViewCuriosityCountries)

        filteredCountries = countryTranslations.keys.sorted().toList()
        updateCountryList()

        // Sobreescriure el botó de tornar enrere
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (scrollCardContainer.visibility == View.VISIBLE) {
                        scrollCardContainer.visibility = View.GONE
                        listViewCountries.visibility = View.VISIBLE
                        searchBar.setText("")
                        filteredCountries = countryTranslations.keys.sorted().toList()
                        updateCountryList()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().lowercase().trim()
                filteredCountries = if (q.isEmpty()) {
                    countryTranslations.keys.sorted().toList()
                } else {
                    countryTranslations.keys.filter { it.contains(q) }.sorted()
                }
                updateCountryList()
            }
        })

        searchBtn.setOnClickListener {
            val input = searchBar.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "Escribe un país", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            searchCountry(input)
        }
    }

    private fun updateCountryList() {
        val displayList = filteredCountries.map { it.replaceFirstChar { c -> c.uppercase() } }
        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.item_country_list,
            R.id.tvCountryItem,
            displayList
        ) {}
        listViewCountries.adapter = adapter

        listViewCountries.setOnItemClickListener { _, _, position, _ ->
            val selected = filteredCountries[position]
            searchBar.setText(selected.replaceFirstChar { it.uppercase() })
            searchCountry(selected)
        }
    }

    private fun searchCountry(nameEs: String) {
        val nameEn = countryTranslations[nameEs.lowercase()]
        if (nameEn == null) {
            Toast.makeText(this, "País no encontrado. Prueba en español.", Toast.LENGTH_SHORT).show()
            return
        }

        loadingText.visibility = View.VISIBLE
        scrollCardContainer.visibility = View.GONE
        listViewCountries.visibility = View.GONE

        val cacheRef = db.getReference("country_info/${nameEn.replace(" ", "_")}")
        cacheRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val capital = snapshot.child("capital").getValue(String::class.java) ?: "-"
                val region = snapshot.child("region").getValue(String::class.java) ?: "-"
                val subregion = snapshot.child("subregion").getValue(String::class.java) ?: "-"
                val population = snapshot.child("population").getValue(Long::class.java) ?: 0L
                val area = snapshot.child("area").getValue(Double::class.java) ?: 0.0
                val languages = snapshot.child("languages").getValue(String::class.java) ?: "-"
                val currency = snapshot.child("currency").getValue(String::class.java) ?: "-"
                val flag = snapshot.child("flag").getValue(String::class.java) ?: ""
                val officialName = snapshot.child("officialName").getValue(String::class.java) ?: nameEn
                val meals = snapshot.child("meals").getValue(String::class.java) ?: "Sin recetas disponibles"

                showData(nameEs, officialName, capital, region, subregion, population, area, languages, currency, flag, meals)
            } else {
                fetchFromApi(nameEs, nameEn, cacheRef)
            }
        }.addOnFailureListener {
            fetchFromApi(nameEs, nameEn, cacheRef)
        }
    }

    private fun fetchFromApi(
        nameEs: String,
        nameEn: String,
        cacheRef: com.google.firebase.database.DatabaseReference
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val infoList = RetrofitClient.countryInfoApi.getCountryInfo(nameEn)
                val info = infoList.firstOrNull() ?: throw Exception("País no encontrado")

                val capital = info.capital?.firstOrNull() ?: "-"
                val region = info.region
                val subregion = info.subregion ?: "-"
                val population = info.population
                val area = info.area ?: 0.0
                val languages = info.languages?.values?.joinToString(", ") ?: "-"
                val currencyEntry = info.currencies?.values?.firstOrNull()
                val currency = if (currencyEntry != null)
                    "${currencyEntry.name} (${currencyEntry.symbol ?: "-"})"
                else "-"
                val flag = info.flags.png ?: ""
                val officialName = info.name.official

                val mealsStr = try {
                    val mealsResponse = RetrofitClient.api.getRecipesByCountry(nameEn)
                    val meals = mealsResponse.meals
                    if (!meals.isNullOrEmpty()) {
                        meals.take(5).joinToString(", ") { it.strMeal }
                    } else "Sin recetas disponibles"
                } catch (e: Exception) { "Sin recetas disponibles" }

                val toSave = mapOf(
                    "capital" to capital,
                    "region" to region,
                    "subregion" to subregion,
                    "population" to population,
                    "area" to area,
                    "languages" to languages,
                    "currency" to currency,
                    "flag" to flag,
                    "officialName" to officialName,
                    "meals" to mealsStr
                )
                cacheRef.setValue(toSave)

                withContext(Dispatchers.Main) {
                    showData(nameEs, officialName, capital, region, subregion, population, area, languages, currency, flag, mealsStr)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingText.visibility = View.GONE
                    listViewCountries.visibility = View.VISIBLE
                    scrollCardContainer.visibility = View.GONE
                    Toast.makeText(this@CountryCuriosityActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showData(
        nameEs: String, officialName: String, capital: String,
        region: String, subregion: String, population: Long,
        area: Double, languages: String, currency: String,
        flag: String, meals: String
    ) {
        loadingText.visibility = View.GONE
        scrollCardContainer.visibility = View.VISIBLE
        listViewCountries.visibility = View.GONE

        tvCountryName.text = nameEs.replaceFirstChar { it.uppercase() }
        tvOfficialName.text = "Nombre oficial: $officialName"
        tvCapital.text = "Capital: $capital"
        tvRegion.text = "Región: $region — $subregion"
        tvPopulation.text = "Población: ${String.format("%,d", population)} hab."
        tvArea.text = "Superficie: ${String.format("%,.0f", area)} km²"
        tvLanguages.text = "Idiomas: $languages"
        tvCurrency.text = "Moneda: $currency"
        tvMeals.text = "Platos típicos: $meals"

        if (flag.isNotEmpty()) {
            Glide.with(this).load(flag).into(flagImage)
            flagImage.visibility = View.VISIBLE
        } else {
            flagImage.visibility = View.GONE
        }
    }
}