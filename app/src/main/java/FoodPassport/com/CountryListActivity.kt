package FoodPassport.com

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CountryListActivity : BaseActivity() {

    private lateinit var searchBar: EditText
    private lateinit var listView: ListView
    private lateinit var tabVisited: TextView
    private lateinit var tabWantToVisit: TextView
    private lateinit var tabMyVisited: TextView
    private lateinit var counter: TextView
    private lateinit var emptyText: TextView

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
        tabMyVisited = findViewById(R.id.tabMyVisited)
        counter = findViewById(R.id.countryCounter)
        emptyText = findViewById(R.id.emptyTextCountry)

        loadFromFirebase { updateAdapter() }

        tabVisited.setOnClickListener {
            currentTab = "visited"
            searchBar.visibility = View.VISIBLE
            updateTabStyle()
            updateAdapter()
        }

        tabWantToVisit.setOnClickListener {
            currentTab = "wantToVisit"
            searchBar.visibility = View.VISIBLE
            updateTabStyle()
            updateAdapter()
        }


        tabMyVisited.setOnClickListener {
            currentTab = "myVisited"

            searchBar.visibility = View.GONE
            searchBar.setText("")
            filteredCountries = allCountries.toList()
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
        db.getReference("user_countries/$uid").get().addOnSuccessListener { snapshot ->
            visitedSet.clear()
            wantToVisitSet.clear()
            snapshot.child("visited").children.forEach {
                visitedSet.add(it.getValue(String::class.java) ?: return@forEach)
            }
            snapshot.child("wantToVisit").children.forEach {
                wantToVisitSet.add(it.getValue(String::class.java) ?: return@forEach)
            }
            onDone()
        }.addOnFailureListener { onDone() }
    }

    private fun updateAdapter() {
        when (currentTab) {
            "myVisited" -> showMyVisited()
            else -> showCheckboxList()
        }
    }


    private fun showCheckboxList() {
        val currentSet = if (currentTab == "visited") visitedSet else wantToVisitSet

        emptyText.visibility = View.GONE
        listView.visibility = View.VISIBLE
        counter.text = "${currentSet.size} de ${allCountries.size} países"
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_multiple_choice,
            filteredCountries
        ) {}

        listView.adapter = adapter

        filteredCountries.forEachIndexed { index, country ->
            listView.setItemChecked(index, currentSet.contains(country))
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val country = filteredCountries[position]
            if (listView.isItemChecked(position)) currentSet.add(country)
            else currentSet.remove(country)
            counter.text = "${currentSet.size} de ${allCountries.size} países"
            saveToFirebase()
        }
    }


    private fun showMyVisited() {
        listView.choiceMode = ListView.CHOICE_MODE_NONE

        val sortedVisited = visitedSet.sorted()

        if (sortedVisited.isEmpty()) {
            listView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
            counter.text = "Aún no has visitado ningún país"
            return
        }

        emptyText.visibility = View.GONE
        listView.visibility = View.VISIBLE
        counter.text = "Has visitado ${sortedVisited.size} países"

        // Llista simple sense checkboxes
        val adapter = ArrayAdapter(
            this,
            R.layout.item_country_list,
            R.id.tvCountryItem,
            sortedVisited
        )
        listView.adapter = adapter


        listView.setOnItemClickListener { _, _, _, _ -> }
    }

    private fun saveToFirebase() {
        db.getReference("user_countries/$uid").setValue(
            mapOf("visited" to visitedSet.toList(), "wantToVisit" to wantToVisitSet.toList())
        )
    }

    private fun updateTabStyle() {
        val activeColor = android.graphics.Color.parseColor("#9E0202")
        val inactiveColor = android.graphics.Color.parseColor("#CCCCCC")
        val darkText = android.graphics.Color.parseColor("#2D2D2D")


        listOf(tabVisited, tabWantToVisit, tabMyVisited).forEach {
            it.setBackgroundColor(inactiveColor)
            it.setTextColor(darkText)
        }


        val activeTab = when (currentTab) {
            "visited" -> tabVisited
            "wantToVisit" -> tabWantToVisit
            else -> tabMyVisited
        }
        activeTab.setBackgroundColor(activeColor)
        activeTab.setTextColor(android.graphics.Color.WHITE)
    }
}