package FoodPassport.com

import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : BaseActivity() {

    private lateinit var tvAvatarPreview: TextView
    private lateinit var editName: EditText
    private lateinit var tvEmail: TextView
    private lateinit var btnSave: Button
    private lateinit var avatarView: AvatarView

    private val db = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_profile)

        findViewById<TextView>(R.id.toolbarTitle).text = "Mi perfil"

        avatarView = findViewById(R.id.avatarView)
        editName = findViewById(R.id.editProfileName)
        tvEmail = findViewById(R.id.tvProfileEmail)
        btnSave = findViewById(R.id.btnSaveProfile)

        // Mostra l'email però no es pot editar
        tvEmail.text = FirebaseAuth.getInstance().currentUser?.email ?: ""

        loadProfile()

        // Actualitza l'avatar en temps real mentre l'usuari escriu el nom
        editName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                avatarView.setName(s.toString())
            }
        })

        btnSave.setOnClickListener { saveProfile() }
    }

    // Carrega el nom actual de Firebase i actualitza l'avatar
    private fun loadProfile() {
        db.getReference("users/$uid").get().addOnSuccessListener { snapshot ->
            val name = snapshot.child("name").getValue(String::class.java) ?: ""
            editName.setText(name)
            avatarView.setName(name)
        }
    }

    // Guarda el nom a Firebase comprovant que no estigui ja en ús
    private fun saveProfile() {
        val newName = editName.text.toString().trim()
        if (newName.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false

        // Comprovar si el nom ja existeix a Firebase
        db.getReference("users").get().addOnSuccessListener { snapshot ->
            val nameTaken = snapshot.children.any { child ->
                val existingName = child.child("name").getValue(String::class.java) ?: ""
                val existingUid = child.key ?: ""
                // Acceptar si el nom coincideix amb el propi usuari (no ha canviat)
                existingName.equals(newName, ignoreCase = true) && existingUid != uid
            }

            if (nameTaken) {
                Toast.makeText(this, "Este nombre ya está en uso, elige otro", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                return@addOnSuccessListener
            }

            // El nom és únic, guardar-lo
            db.getReference("users/$uid").updateChildren(mapOf("name" to newName))
                .addOnSuccessListener {
                    Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al verificar el nombre", Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }
    }
}