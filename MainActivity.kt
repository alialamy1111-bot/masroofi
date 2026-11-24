package com.masroofi.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF167D67)
private val Ink = Color(0xFF182522)
private val Muted = Color(0xFF77827F)
private val Canvas = Color(0xFFF5F7F6)

data class Tx(val id: Long, val amount: Double, val currency: String, val kind: String, val category: String, val note: String, val date: String)

class ExpenseDb(context: Context) : SQLiteOpenHelper(context, "masroofi.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, amount REAL NOT NULL, currency TEXT NOT NULL, kind TEXT NOT NULL, category TEXT NOT NULL, note TEXT NOT NULL, date TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun all(): List<Tx> {
        val out = mutableListOf<Tx>()
        readableDatabase.rawQuery("SELECT id,amount,currency,kind,category,note,date FROM transactions ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out += Tx(c.getLong(0), c.getDouble(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6))
        }
        return out
    }
    fun insert(amount: Double, currency: String, kind: String, category: String, note: String) {
        val v = ContentValues().apply {
            put("amount", amount); put("currency", currency); put("kind", kind); put("category", category); put("note", note)
            put("date", SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date()))
        }
        writableDatabase.insert("transactions", null, v)
    }
    fun delete(id: Long) { writableDatabase.delete("transactions", "id=?", arrayOf(id.toString())) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = ExpenseDb(this)
        setContent { MasroofiApp(db) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasroofiApp(db: ExpenseDb) {
    var records by remember { mutableStateOf(db.all()) }
    var selectedCurrency by remember { mutableStateOf("IQD") }
    var showAdd by remember { mutableStateOf(false) }
    val currencies = listOf("IQD", "USD", "EUR", "SAR", "AED", "TRY", "GBP", "KWD", "Other")
    val filtered = records.filter { it.currency == selectedCurrency }
    val income = filtered.filter { it.kind == "IN" }.sumOf { it.amount }
    val expenses = filtered.filter { it.kind == "OUT" }.sumOf { it.amount }
    val fmt = DecimalFormat("#,##0.##")

    MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas, surface = Color.White)) {
        Scaffold(containerColor = Canvas, floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = Green, contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "إضافة عملية")
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(Green, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ReceiptLong, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("مصروفي", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("إدارة أموالك ببساطة", fontSize = 12.sp, color = Muted)
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(color = Color(0xFFE4F2ED), shape = RoundedCornerShape(30.dp)) {
                        Text("● دون إنترنت", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("العملة", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(value = currencyName(selectedCurrency), onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFDCE4E0)))
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        currencies.forEach { c -> DropdownMenuItem(text = { Text(currencyName(c)) }, onClick = { selectedCurrency = c; expanded = false }) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Surface(color = Green, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("الرصيد الصافي", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
                        Spacer(Modifier.height(7.dp))
                        Text("${fmt.format(income - expenses)} ${symbol(selectedCurrency)}", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SummaryTile("الإيرادات", income, selectedCurrency, true, Modifier.weight(1f))
                            SummaryTile("المصروفات", expenses, selectedCurrency, false, Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("سجل العمليات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.weight(1f))
                    Text("${filtered.size} عملية", color = Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                if (filtered.isEmpty()) {
                    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("لا توجد عمليات لهذه العملة بعد", color = Muted, fontSize = 14.sp)
                        Text("اضغط + لإضافة أول عملية", color = Green, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(filtered, key = { it.id }) { tx ->
                            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(42.dp).background(if (tx.kind == "IN") Color(0xFFE4F3EC) else Color(0xFFFFEFEB), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                                        Text(if (tx.kind == "IN") "↙" else "↗", color = if (tx.kind == "IN") Green else Color(0xFFD46B51), fontSize = 22.sp)
                                    }
                                    Spacer(Modifier.width(11.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(tx.category, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                                        Text(if (tx.note.isBlank()) tx.date else "${tx.note} • ${tx.date}", color = Muted, fontSize = 10.sp, maxLines = 2)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${if (tx.kind == "IN") "+" else "−"}${fmt.format(tx.amount)}", color = if (tx.kind == "IN") Green else Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(symbol(tx.currency), color = Muted, fontSize = 10.sp)
                                    }
                                    IconButton(onClick = { db.delete(tx.id); records = db.all() }) { Icon(Icons.Default.Delete, "حذف", tint = Muted) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showAdd) {
        AddTransactionDialog(currencies = currencies, initialCurrency = selectedCurrency, onDismiss = { showAdd = false }, onSave = { amount, currency, kind, category, note ->
            db.insert(amount, currency, kind, category, note); records = db.all(); selectedCurrency = currency; showAdd = false
        })
    }
}

@Composable
fun SummaryTile(title: String, value: Double, currency: String, positive: Boolean, modifier: Modifier = Modifier) {
    Surface(color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
            Spacer(Modifier.height(5.dp))
            Text("${DecimalFormat("#,##0.##").format(value)} ${symbol(currency)}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(currencies: List<String>, initialCurrency: String, onDismiss: () -> Unit, onSave: (Double, String, String, String, String) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(initialCurrency) }
    var kind by remember { mutableStateOf("OUT") }
    var category by remember { mutableStateOf("طعام") }
    var note by remember { mutableStateOf("") }
    var currencyExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val categories = if (kind == "OUT") listOf("طعام", "مشتريات", "منزل", "سيارة", "وقود", "فواتير", "صحة", "تعليم", "ترفيه", "سفر", "أخرى") else listOf("راتب", "عمل إضافي", "بيع", "هدية", "استثمار", "أخرى")
    AlertDialog(onDismissRequest = onDismiss, title = { Text("إضافة عملية", fontWeight = FontWeight.Bold) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kind == "OUT", onClick = { kind = "OUT"; category = "طعام" }, label = { Text("مصروف") })
                FilterChip(selected = kind == "IN", onClick = { kind = "IN"; category = "راتب" }, label = { Text("إيراد") })
            }
            OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("المبلغ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = !currencyExpanded }) {
                OutlinedTextField(value = currencyName(currency), onValueChange = {}, readOnly = true, label = { Text("العملة") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) { currencies.forEach { c -> DropdownMenuItem(text = { Text(currencyName(c)) }, onClick = { currency = c; currencyExpanded = false }) } }
            }
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                OutlinedTextField(value = category, onValueChange = {}, readOnly = true, label = { Text("التصنيف") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) { categories.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { category = c; categoryExpanded = false }) } }
            }
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("ملاحظة (اختياري)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
    }, confirmButton = {
        Button(onClick = { val amount = amountText.replace(',', '.').toDoubleOrNull(); if (amount != null && amount > 0) onSave(amount, currency, kind, category, note) }, enabled = (amountText.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0) { Text("حفظ العملية") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

fun currencyName(code: String) = when (code) {
    "IQD" -> "الدينار العراقي (IQD)"; "USD" -> "الدولار الأمريكي (USD)"; "EUR" -> "اليورو (EUR)"; "SAR" -> "الريال السعودي (SAR)"; "AED" -> "الدرهم الإماراتي (AED)"; "TRY" -> "الليرة التركية (TRY)"; "GBP" -> "الجنيه الإسترليني (GBP)"; "KWD" -> "الدينار الكويتي (KWD)"; else -> "أخرى (Other)"
}
fun symbol(code: String) = when (code) { "IQD" -> "د.ع"; "USD" -> "$"; "EUR" -> "€"; "SAR" -> "ر.س"; "AED" -> "د.إ"; "TRY" -> "₺"; "GBP" -> "£"; "KWD" -> "د.ك"; else -> code }
