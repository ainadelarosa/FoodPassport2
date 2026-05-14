package FoodPassport.com

import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : BaseActivity() {

    private lateinit var avatarView: AvatarView
    private lateinit var editName: EditText
    private lateinit var tvEmail: TextView
    private lateinit var btnSave: Button

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

        tvEmail.text = FirebaseAuth.getInstance().currentUser?.email ?: ""

        loadProfile()


        editName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                avatarView.setName(s.toString())
            }
        })

        btnSave.setOnClickListener { saveProfile() }
    }

    private fun loadProfile() {
        db.getReference("users/$uid").get().addOnSuccessListener { snapshot ->
            val name = snapshot.child("name").getValue(String::class.java) ?: ""
            editName.setText(name)

            avatarView.setName(name)
        }
    }

    private fun saveProfile() {
        val newName = editName.text.toString().trim()
        if (newName.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false

        db.getReference("users/$uid").updateChildren(mapOf("name" to newName))
            .addOnSuccessListener {
                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
            }
    }
}