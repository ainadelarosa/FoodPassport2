package FoodPassport.com

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CountryListActivity : BaseActivity() {

    private lateinit var searchBar: EditText
    private lateinit var listView: ListView
    private lateinit var tabVisited: TextView
    private lateinit var tabWantToVisit: TextView
    private lateinit var counter: TextView

    private val db = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private var currentTab = "visited"
    private var visitedSet = mutableSetOf<String>()
    private var wantToVisitSet = mutableSetOf<String>()
    private var filteredCountries = allCountries.toList()

    companion object {
        val allCountries = listOf(
            "Afganistán", "Albania", "Alemania", "Andorra", "Angola", "Antigua y Barbuda",
            "Arabia Saudita", "Argelia", "Argentina", "Armenia", "Australia", "Austria",
            "Azerbaiyán", "Bahamas", "Bahrein", "Bangladés", "Barbados", "Bélgica",
            "Belice", "Benín", "Bielorrusia", "Bolivia", "Bosnia y Herzegovina", "Botsuana",
            "Brasil", "Brunéi", "Bulgaria", "Burkina Faso", "Burundi", "Bután",
            "Cabo Verde", "Camboya", "Camerún", "Canadá", "Catar", "Chad",
            "Chile", "China", "Chipre", "Colombia", "Comoras", "Corea del Norte",
            "Corea del Sur", "Costa de Marfil", "Costa Rica", "Croacia", "Cuba", "Dinamarca",
            "Dominica", "Ecuador", "Egipto", "El Salvador", "Emiratos Árabes Unidos", "Eritrea",
            "Eslovaquia", "Eslovenia", "España", "Estados Unidos", "Estonia", "Etiopía",
            "Filipinas", "Finlandia", "Fiyi", "Francia", "Gabón", "Gambia",
            "Georgia", "Ghana", "Granada", "Grecia", "Guatemala", "Guinea",
            "Guinea Ecuatorial", "Guinea-Bisáu", "Guyana", "Haití", "Honduras", "Hungría",
            "India", "Indonesia", "Irak", "Irán", "Irlanda", "Islandia",
            "Islas Marshall", "Islas Salomón", "Israel", "Italia", "Jamaica", "Japón",
            "Jordania", "Kazajistán", "Kenia", "Kirguistán", "Kiribati", "Kuwait",
            "Laos", "Lesoto", "Letonia", "Líbano", "Liberia", "Libia",
            "Liechtenstein", "Lituania", "Luxemburgo", "Madagascar", "Malasia", "Malaui",
            "Maldivas", "Malí", "Malta", "Marruecos", "Mauricio", "Mauritania",
            "México", "Micronesia", "Moldavia", "Mónaco", "Mongolia", "Montenegro",
            "Mozambique", "Namibia", "Nauru", "Nepal", "Nicaragua", "Níger",
            "Nigeria", "Noruega", "Nueva Zelanda", "Omán", "Países Bajos", "Pakistán",
            "Palaos", "Panamá", "Papúa Nueva Guinea", "Paraguay", "Perú", "Polonia",
            "Portugal", "Reino Unido", "República Centroafricana", "República Checa",
            "República del Congo", "República Democrática del Congo", "República Dominicana",
            "Ruanda", "Rumania", "Rusia", "Samoa", "San Cristóbal y Nieves",
            "San Marino", "San Vicente y las Granadinas", "Santa Lucía", "Santo Tomé y Príncipe",
            "Senegal", "Serbia", "Seychelles", "Sierra Leona", "Singapur", "Siria",
            "Somalia", "Sri Lanka", "Suazilandia", "Sudáfrica", "Sudán", "Sudán del Sur",
            "Suecia", "Suiza", "Surinam", "Tailandia", "Tanzania", "Tayikistán",
            "Timor Oriental", "Togo", "Tonga", "Trinidad y Tobago", "Túnez", "Turkmenistán",
            "Turquía", "Tuvalu", "Ucrania", "Uganda", "Uruguay", "Uzbekistán",
            "Vanuatu", "Venezuela", "Vietnam", "Yemen", "Yibuti", "Zambia", "Zimbabue"
        ).sorted()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_country_list)

        findViewById<TextView>(R.id.toolbarTitle).text = "Mis países"

        searchBar = findViewById(R.id.searchBarCountryList)
        listView = findViewById(R.id.listViewCountryList)
        tabVisited = findViewById(R.id.tabVisited)
        tabWantToVisit = findViewById(R.id.tabWantToVisit)
        counter = findViewById(R.id.countryCounter)

        loadFromFirebase {
            updateAdapter()
        }

        tabVisited.setOnClickListener {
            currentTab = "visited"
            updateTabStyle()
            updateAdapter()
        }

        tabWantToVisit.setOnClickListener {
            currentTab = "wantToVisit"
            updateTabStyle()
            updateAdapter()
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().lowercase()
                filteredCountries = allCountries.filter { it.lowercase().contains(q) }
                updateAdapter()
            }
        })

        updateTabStyle()
    }

    private fun loadFromFirebase(onDone: () -> Unit) {
        val ref = db.getReference("user_countries/$uid")
        ref.get().addOnSuccessListener { snapshot ->
            visitedSet.clear()
            wantToVisitSet.clear()
            snapshot.child("visited").children.forEach {
                val name = it.getValue(String::class.java) ?: return@forEach
                visitedSet.add(name)
            }
            snapshot.child("wantToVisit").children.forEach {
                val name = it.getValue(String::class.java) ?: return@forEach
                wantToVisitSet.add(name)
            }
            onDone()
        }.addOnFailureListener { onDone() }
    }

    private fun updateAdapter() {
        val currentSet = if (currentTab == "visited") visitedSet else wantToVisitSet

        counter.text = "${currentSet.size} de ${allCountries.size} países"

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_multiple_choice,
            filteredCountries
        ) {}

        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        filteredCountries.forEachIndexed { index, country ->
            listView.setItemChecked(index, currentSet.contains(country))
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val country = filteredCountries[position]
            val isChecked = listView.isItemChecked(position)

            if (isChecked) {
                currentSet.add(country)
            } else {
                currentSet.remove(country)
            }

            counter.text = "${currentSet.size} de ${allCountries.size} países"
            saveToFirebase()
        }
    }

    private fun saveToFirebase() {
        val ref = db.getReference("user_countries/$uid")
        val data = mapOf(
            "visited" to visitedSet.toList(),
            "wantToVisit" to wantToVisitSet.toList()
        )
        ref.setValue(data)
    }

    private fun updateTabStyle() {
        val activeColor = android.graphics.Color.parseColor("#9E0202")
        val inactiveColor = android.graphics.Color.parseColor("#CCCCCC")

        if (currentTab == "visited") {
            tabVisited.setBackgroundColor(activeColor)
            tabVisited.setTextColor(android.graphics.Color.WHITE)
            tabWantToVisit.setBackgroundColor(inactiveColor)
            tabWantToVisit.setTextColor(android.graphics.Color.parseColor("#2D2D2D"))
        } else {
            tabWantToVisit.setBackgroundColor(activeColor)
            tabWantToVisit.setTextColor(android.graphics.Color.WHITE)
            tabVisited.setBackgroundColor(inactiveColor)
            tabVisited.setTextColor(android.graphics.Color.parseColor("#2D2D2D"))
        }
    }
}