package FoodPassport.com

import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CreateGroupActivity : BaseActivity() {

    private lateinit var editGroupName: EditText
    private lateinit var editUsername: EditText
    private lateinit var btnAddUser: Button
    private lateinit var btnCreateGroup: Button
    private lateinit var membersList: ListView

    private val db = FirebaseDatabase
        .getInstance("https://foodpassport-40192-default-rtdb.firebaseio.com")
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val members = mutableMapOf<String, String>()
    private var isCreating = false
    private var myName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDrawer(R.layout.activity_create_group)

        findViewById<TextView>(R.id.toolbarTitle).text = "Crear grupo"

        editGroupName = findViewById(R.id.editGroupName)
        editUsername = findViewById(R.id.editUsername)
        btnAddUser = findViewById(R.id.btnAddUser)
        btnCreateGroup = findViewById(R.id.btnCreateGroup)
        membersList = findViewById(R.id.listViewMembers)

        db.getReference("users/$uid/name").get().addOnSuccessListener { snap ->
            myName = snap.getValue(String::class.java) ?: "Usuario"
            members[uid] = "$myName (tú)"
            updateMembersList()
        }

        btnAddUser.setOnClickListener {
            val username = editUsername.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(this, "Escribe un nombre de usuario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            searchUserByName(username)
        }

        btnCreateGroup.setOnClickListener {
            createGroup()
        }
    }

    private fun searchUserByName(name: String) {
        db.getReference("users").get().addOnSuccessListener { snapshot ->

            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                Toast.makeText(this, "No hay usuarios registrados", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            var found = false

            snapshot.children.forEach { child ->
                val userName = child.child("name").getValue(String::class.java)?.trim() ?: ""
                val userId = child.key ?: ""

                if (userName.equals(name.trim(), ignoreCase = true)) {
                    found = true

                    if (userId == uid) {
                        Toast.makeText(this, "No puedes añadirte a ti mismo", Toast.LENGTH_SHORT).show()
                        return@forEach
                    }

                    if (members.containsKey(userId)) {
                        Toast.makeText(this, "$userName ya está en el grupo", Toast.LENGTH_SHORT).show()
                        return@forEach
                    }

                    members[userId] = userName
                    updateMembersList()
                    editUsername.setText("")
                    Toast.makeText(this, "✓ $userName añadido", Toast.LENGTH_SHORT).show()
                }
            }

            if (!found) {
                val availableNames = snapshot.children
                    .mapNotNull { it.child("name").getValue(String::class.java) }
                    .joinToString(", ")
                Toast.makeText(
                    this,
                    "Usuario '$name' no encontrado.\nDisponibles: $availableNames",
                    Toast.LENGTH_LONG
                ).show()
            }

        }.addOnFailureListener { error ->
            Toast.makeText(this, "Error al buscar: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateMembersList() {
        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.item_country_list,
            R.id.tvCountryItem,
            members.values.toList()
        ) {}
        membersList.adapter = adapter
    }

    private fun createGroup() {
        if (isCreating) return
        val groupName = editGroupName.text.toString().trim()
        if (groupName.isEmpty()) {
            Toast.makeText(this, "Escribe un nombre para el grupo", Toast.LENGTH_SHORT).show()
            return
        }
        if (members.size < 2) {
            Toast.makeText(this, "Añade al menos un miembro", Toast.LENGTH_SHORT).show()
            return
        }

        isCreating = true
        btnCreateGroup.isEnabled = false

        val membersSnapshot = members.toMap()
        val groupRef = db.getReference("groups").push()
        val groupId = groupRef.key ?: run {
            isCreating = false
            btnCreateGroup.isEnabled = true
            return
        }

        // Només l'usuari creador entra directament al grup
        // La resta reben una invitació
        val groupData = mapOf(
            "info" to mapOf(
                "name" to groupName,
                "createdBy" to uid,
                "createdAt" to System.currentTimeMillis()
            ),
            "members" to mapOf(uid to true)
        )

        groupRef.setValue(groupData).addOnSuccessListener {
            // Afegir el grup a l'usuari creador
            db.getReference("user_groups/$uid/$groupId").setValue(true)

            // Enviar invitació a la resta de membres
            val otherMembers = membersSnapshot.filter { it.key != uid }
            var sentCount = 0

            if (otherMembers.isEmpty()) {
                Toast.makeText(this, "Grupo '$groupName' creado", Toast.LENGTH_SHORT).show()
                finish()
                return@addOnSuccessListener
            }

            otherMembers.forEach { (memberId, memberName) ->
                val invite = mapOf(
                    "groupName" to groupName,
                    "invitedBy" to myName,
                    "invitedAt" to System.currentTimeMillis()
                )
                db.getReference("group_invites/$memberId/$groupId")
                    .setValue(invite)
                    .addOnSuccessListener {
                        sentCount++
                        if (sentCount == otherMembers.size) {
                            Toast.makeText(
                                this,
                                "Grupo creado. Invitaciones enviadas a ${otherMembers.values.joinToString(", ")}",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(this, "Error enviando invitación: ${error.message}", Toast.LENGTH_LONG).show()
                        isCreating = false
                        btnCreateGroup.isEnabled = true
                    }
            }
        }.addOnFailureListener { error ->
            Toast.makeText(this, "Error al crear el grupo: ${error.message}", Toast.LENGTH_SHORT).show()
            isCreating = false
            btnCreateGroup.isEnabled = true
        }
    }
}