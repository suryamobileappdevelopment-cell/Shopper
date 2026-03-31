package com.example.shopper

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Locale

data class Product(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = ""
)

data class CartItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Long = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {

    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val userId = auth.currentUser?.uid.orEmpty()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var products by remember { mutableStateOf(listOf<Product>()) }
    var cartItems by remember { mutableStateOf(listOf<CartItem>()) }

    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    var isLoadingProducts by remember { mutableStateOf(true) }
    var isLoadingCart by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val cartCount = remember(cartItems) {
        cartItems.sumOf { it.quantity.toInt() }
    }

    val totalAmount = remember(cartItems) {
        cartItems.sumOf { it.price * it.quantity.toDouble() }
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE3F2FD),
            Color(0xFFFFF8E1)
        )
    )

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct().sorted()
    }

    val filteredProducts = remember(products, searchText, selectedCategory) {
        products.filter { product ->
            val matchesCategory =
                selectedCategory == "All" || product.category.equals(selectedCategory, ignoreCase = true)

            val matchesSearch =
                product.name.contains(searchText, ignoreCase = true) ||
                        product.category.contains(searchText, ignoreCase = true) ||
                        product.description.contains(searchText, ignoreCase = true)

            matchesCategory && matchesSearch
        }
    }

    DisposableEffect(userId) {
        val productListener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = error.message ?: "Failed to load products"
                    isLoadingProducts = false
                    return@addSnapshotListener
                }

                products = snapshot?.documents?.map { doc ->
                    Product(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        category = doc.getString("category") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: ""
                    )
                } ?: emptyList()

                isLoadingProducts = false
            }

        val cartListener =
            if (userId.isNotEmpty()) {
                db.collection("users")
                    .document(userId)
                    .collection("cart")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            errorMessage = error.message ?: "Failed to load cart"
                            isLoadingCart = false
                            return@addSnapshotListener
                        }

                        cartItems = snapshot?.documents?.map { doc ->
                            CartItem(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                quantity = doc.getLong("quantity") ?: 1
                            )
                        } ?: emptyList()

                        isLoadingCart = false
                    }
            } else {
                isLoadingCart = false
                null
            }

        onDispose {
            productListener.remove()
            cartListener?.remove()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Shopper",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Store",
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            auth.signOut()
                            Toast.makeText(
                                context,
                                "Logged out successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    ) {
                        Text(
                            text = "Logout",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF90CAF9),
                    titleContentColor = Color.Black
                ),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when {
                isLoadingProducts || isLoadingCart -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading products...")
                    }
                }

                errorMessage.isNotEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Discover Products",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Cart Items: $cartCount",
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            label = { Text("Search products") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                AssistChip(
                                    onClick = { selectedCategory = category },
                                    label = { Text(category) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (selectedCategory == category) {
                                            Color(0xFF64B5F6)
                                        } else {
                                            Color.White
                                        }
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (filteredProducts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No products found")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredProducts) { product ->
                                    ProductCard(
                                        product = product,
                                        onAddToCart = {
                                            if (userId.isEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    "Please login first",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                addToCart(
                                                    db = db,
                                                    userId = userId,
                                                    product = product,
                                                    onResult = { message ->
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(message)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        CartSection(
                            cartItems = cartItems,
                            totalAmount = totalAmount,
                            onIncrease = { item ->
                                updateCartQuantity(
                                    db = db,
                                    userId = userId,
                                    cartItemId = item.id,
                                    newQuantity = item.quantity + 1,
                                    onResult = { message ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                )
                            },
                            onDecrease = { item ->
                                if (item.quantity > 1) {
                                    updateCartQuantity(
                                        db = db,
                                        userId = userId,
                                        cartItemId = item.id,
                                        newQuantity = item.quantity - 1,
                                        onResult = { message ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(message)
                                            }
                                        }
                                    )
                                } else {
                                    removeFromCart(
                                        db = db,
                                        userId = userId,
                                        cartItemId = item.id,
                                        onResult = { message ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(message)
                                            }
                                        }
                                    )
                                }
                            },
                            onRemoveItem = { item ->
                                removeFromCart(
                                    db = db,
                                    userId = userId,
                                    cartItemId = item.id,
                                    onResult = { message ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                )
                            },
                            onCheckout = {
                                if (userId.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "Please login first",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (cartItems.isEmpty()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Cart is empty")
                                    }
                                } else {
                                    checkoutItems(
                                        db = db,
                                        userId = userId,
                                        cartItems = cartItems,
                                        totalAmount = totalAmount,
                                        onResult = { message ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(message)
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = product.category,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = product.description)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "£${String.format(Locale.UK, "%.2f", product.price)}",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1565C0)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAddToCart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to Cart")
            }
        }
    }
}

@Composable
fun CartSection(
    cartItems: List<CartItem>,
    totalAmount: Double,
    onIncrease: (CartItem) -> Unit,
    onDecrease: (CartItem) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onCheckout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "My Cart",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (cartItems.isEmpty()) {
                Text("Your cart is empty")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF7F7F7)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text("Price: £${String.format(Locale.UK, "%.2f", item.price)}")
                                Text(
                                    text = "Subtotal: £${
                                        String.format(
                                            Locale.UK,
                                            "%.2f",
                                            item.price * item.quantity.toDouble()
                                        )
                                    }"
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(onClick = { onDecrease(item) }) {
                                        Text("-")
                                    }

                                    Text(
                                        text = item.quantity.toString(),
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(onClick = { onIncrease(item) }) {
                                        Text("+")
                                    }

                                    TextButton(onClick = { onRemoveItem(item) }) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Total: £${String.format(Locale.UK, "%.2f", totalAmount)}",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onCheckout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Checkout")
                }
            }
        }
    }
}

fun addToCart(
    db: FirebaseFirestore,
    userId: String,
    product: Product,
    onResult: (String) -> Unit
) {
    val cartRef = db.collection("users")
        .document(userId)
        .collection("cart")
        .document(product.id)

    cartRef.get()
        .addOnSuccessListener { doc ->
            if (doc.exists()) {
                val currentQuantity = doc.getLong("quantity") ?: 1
                cartRef.update("quantity", currentQuantity + 1)
                    .addOnSuccessListener { onResult("${product.name} added to cart") }
                    .addOnFailureListener { e -> onResult(e.message ?: "Failed to update cart") }
            } else {
                val cartData = hashMapOf(
                    "name" to product.name,
                    "price" to product.price,
                    "quantity" to 1
                )

                cartRef.set(cartData)
                    .addOnSuccessListener { onResult("${product.name} added to cart") }
                    .addOnFailureListener { e -> onResult(e.message ?: "Failed to add to cart") }
            }
        }
        .addOnFailureListener { e ->
            onResult(e.message ?: "Failed to access cart")
        }
}

fun updateCartQuantity(
    db: FirebaseFirestore,
    userId: String,
    cartItemId: String,
    newQuantity: Long,
    onResult: (String) -> Unit
) {
    db.collection("users")
        .document(userId)
        .collection("cart")
        .document(cartItemId)
        .update("quantity", newQuantity)
        .addOnSuccessListener { onResult("Cart updated") }
        .addOnFailureListener { e -> onResult(e.message ?: "Failed to update quantity") }
}

fun removeFromCart(
    db: FirebaseFirestore,
    userId: String,
    cartItemId: String,
    onResult: (String) -> Unit
) {
    db.collection("users")
        .document(userId)
        .collection("cart")
        .document(cartItemId)
        .delete()
        .addOnSuccessListener { onResult("Item removed from cart") }
        .addOnFailureListener { e -> onResult(e.message ?: "Failed to remove item") }
}

fun checkoutItems(
    db: FirebaseFirestore,
    userId: String,
    cartItems: List<CartItem>,
    totalAmount: Double,
    onResult: (String) -> Unit
) {
    val orderRef = db.collection("users")
        .document(userId)
        .collection("orders")
        .document()

    val items = cartItems.map { item ->
        hashMapOf(
            "id" to item.id,
            "name" to item.name,
            "price" to item.price,
            "quantity" to item.quantity
        )
    }

    val orderData = hashMapOf(
        "items" to items,
        "totalAmount" to totalAmount,
        "itemCount" to cartItems.size,
        "createdAt" to FieldValue.serverTimestamp(),
        "status" to "Placed"
    )

    orderRef.set(orderData)
        .addOnSuccessListener {
            val batch = db.batch()

            cartItems.forEach { item ->
                val cartDoc = db.collection("users")
                    .document(userId)
                    .collection("cart")
                    .document(item.id)
                batch.delete(cartDoc)
            }

            batch.commit()
                .addOnSuccessListener {
                    onResult("Order placed successfully")
                }
                .addOnFailureListener { e ->
                    onResult(e.message ?: "Order placed, but cart clear failed")
                }
        }
        .addOnFailureListener { e ->
            onResult(e.message ?: "Checkout failed")
        }
}