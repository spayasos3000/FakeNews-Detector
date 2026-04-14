package com.tudominio.fakenewsdetector.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tudominio.fakenewsdetector.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkCurrentSession()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                
                if (user != null) {
                    val userData = getUserDataFromFirestore(user.uid)
                    _currentUser.value = userData
                    _authState.value = AuthState.Success(userData)
                } else {
                    _authState.value = AuthState.Error("Error al iniciar sesión")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                
                if (user != null) {
                    // Crear documento en Firestore con role = "user"
                    val newUser = User(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = displayName,
                        role = "user"
                    )
                    
                    firestore.collection("users")
                        .document(user.uid)
                        .set(newUser)
                        .await()
                    
                    _currentUser.value = newUser
                    _authState.value = AuthState.Success(newUser)
                } else {
                    _authState.value = AuthState.Error("Error al crear cuenta")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                
                if (user != null) {
                    // Verificar si es la primera vez (no existe documento en Firestore)
                    val userDoc = firestore.collection("users")
                        .document(user.uid)
                        .get()
                        .await()
                    
                    val userData = if (userDoc.exists()) {
                        userDoc.toObject(User::class.java) ?: User()
                    } else {
                        // Primera vez, crear documento con role = "user"
                        val newUser = User(
                            uid = user.uid,
                            email = user.email ?: "",
                            displayName = user.displayName ?: "",
                            role = "user"
                        )
                        
                        firestore.collection("users")
                            .document(user.uid)
                            .set(newUser)
                            .await()
                        
                        newUser
                    }
                    
                    _currentUser.value = userData
                    _authState.value = AuthState.Success(userData)
                } else {
                    _authState.value = AuthState.Error("Error al iniciar sesión con Google")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _currentUser.value = null
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al cerrar sesión")
            }
        }
    }

    fun checkCurrentSession() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                
                if (currentUser != null) {
                    val userData = getUserDataFromFirestore(currentUser.uid)
                    _currentUser.value = userData
                    _authState.value = AuthState.Success(userData)
                } else {
                    _authState.value = AuthState.Idle
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Idle
            }
        }
    }

    private suspend fun getUserDataFromFirestore(uid: String): User {
        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            if (document.exists()) {
                document.toObject(User::class.java) ?: User()
            } else {
                User(uid = uid, role = "user")
            }
        } catch (e: Exception) {
            User(uid = uid, role = "user")
        }
    }

    fun getGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(com.tudominio.fakenewsdetector.BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }
}
