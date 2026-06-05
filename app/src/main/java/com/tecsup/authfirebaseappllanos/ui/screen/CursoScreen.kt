package com.tecsup.authfirebaseappllanos.entities

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.authfirebaseappllanos.Entities.Curso
import com.tecsup.authfirebaseappllanos.ui.screen.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursosScreen(onLogout: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val userId = auth.currentUser?.uid ?: ""
    val userEmail = auth.currentUser?.email ?: "Usuario"

    var cursos by remember { mutableStateOf(listOf<Curso>()) }
    var showDialog by remember { mutableStateOf(false) }
    var cursoEditando by remember { mutableStateOf<Curso?>(null) }
    var listVisible by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        delay(200)
        listVisible = true
        db.collection("cursos")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    cursos = snapshot.documents.map { doc ->
                        Curso(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "",
                            descripcion = doc.getString("descripcion") ?: "",
                            creditos = (doc.getLong("creditos") ?: 0).toInt(),
                            userId = doc.getString("userId") ?: ""
                        )
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis Cursos", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text(userEmail, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EduBlue),
                actions = {
                    IconButton(onClick = {
                        auth.signOut()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { cursoEditando = null; showDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo curso") },
                containerColor = EduBlue,
                contentColor = Color.White
            )
        },
        containerColor = EduWhite
    ) { padding ->

        if (cursos.isEmpty() && listVisible) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(EduBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = EduBlueMid, modifier = Modifier.size(44.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Sin cursos aún", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EduBlue)
                    Text("Presiona el botón para agregar", color = EduGray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header stats
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.horizontalGradient(listOf(EduBlue, EduBlueMid)))
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total de cursos", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    Text("${cursos.size}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                    Text("registrados", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Créditos totales", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    Text("${cursos.sumOf { it.creditos }}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                    Text("acumulados", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                itemsIndexed(cursos) { index, curso ->
                    AnimatedCursoCard(
                        curso = curso,
                        index = index,
                        visible = listVisible,
                        onEditar = { cursoEditando = curso; showDialog = true },
                        onEliminar = {
                            db.collection("cursos").document(curso.id).delete()
                            Toast.makeText(context, "Curso eliminado", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showDialog) {
        CursoDialog(
            cursoInicial = cursoEditando,
            onDismiss = { showDialog = false },
            onGuardar = { nombre, descripcion, creditos ->
                if (cursoEditando == null) {
                    db.collection("cursos").add(
                        hashMapOf("nombre" to nombre, "descripcion" to descripcion, "creditos" to creditos, "userId" to userId)
                    )
                    Toast.makeText(context, "Curso agregado ✅", Toast.LENGTH_SHORT).show()
                } else {
                    db.collection("cursos").document(cursoEditando!!.id)
                        .update("nombre", nombre, "descripcion", descripcion, "creditos", creditos)
                    Toast.makeText(context, "Curso actualizado ✅", Toast.LENGTH_SHORT).show()
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun AnimatedCursoCard(
    curso: Curso,
    index: Int,
    visible: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        delay(index * 80L)
        cardVisible = true
    }

    AnimatedVisibility(
        visible = cardVisible,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar con inicial
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EduBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = curso.nombre.take(1).uppercase(),
                        color = EduBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(curso.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EduBlue)
                    if (curso.descripcion.isNotBlank()) {
                        Text(curso.descripcion, fontSize = 12.sp, color = EduGray, maxLines = 1)
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EduGreenLight)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("${curso.creditos} créditos", fontSize = 11.sp, color = EduGreen, fontWeight = FontWeight.SemiBold)
                    }
                }

                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = EduBlueMid, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFD84040), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun CursoDialog(
    cursoInicial: Curso?,
    onDismiss: () -> Unit,
    onGuardar: (String, String, Int) -> Unit
) {
    var nombre by remember { mutableStateOf(cursoInicial?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(cursoInicial?.descripcion ?: "") }
    var creditos by remember { mutableStateOf(cursoInicial?.creditos?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(EduBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (cursoInicial == null) Icons.Default.Add else Icons.Default.Edit,
                        contentDescription = null, tint = EduBlue, modifier = Modifier.size(20.dp)
                    )
                }
                Text(if (cursoInicial == null) "Nuevo curso" else "Editar curso", fontWeight = FontWeight.Bold, color = EduBlue)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del curso") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EduBlue, focusedLabelColor = EduBlue, cursorColor = EduBlue)
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EduBlue, focusedLabelColor = EduBlue, cursorColor = EduBlue)
                )
                OutlinedTextField(
                    value = creditos,
                    onValueChange = { creditos = it.filter { c -> c.isDigit() } },
                    label = { Text("Créditos") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EduBlue, focusedLabelColor = EduBlue, cursorColor = EduBlue)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(nombre, descripcion, creditos.toIntOrNull() ?: 0) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EduBlue)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = EduGray) }
        }
    )
}