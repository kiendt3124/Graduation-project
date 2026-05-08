package com.uet.expensetracker.features.money.ui.detailtransaction

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.model.Contact
import com.uet.expensetracker.features.money.ui.addtransaction.components.ContactChip
import com.uet.expensetracker.features.money.ui.detailtransaction.components.DetailTransactionTopBar
import com.uet.expensetracker.features.money.ui.detailtransaction.components.TransactionDetailItem
import java.text.SimpleDateFormat
import java.util.*
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen
import com.uet.expensetracker.utils.getStringResId
import com.uet.expensetracker.utils.formatAmountWithCurrency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.AppColor.Light.PrimaryColor.cardColor
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextSecondary
import com.uet.expensetracker.utils.getDrawableResId

@Composable
fun DetailTransactionScreen(
    transactionId: Int,
    viewModel: DetailTransactionViewModel = hiltViewModel(),
    navController: NavController = rememberNavController()
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load transaction when screen is first displayed
    LaunchedEffect(transactionId) {
        viewModel.processIntent(DetailTransactionIntent.LoadTransaction(transactionId))
    }

    // Handle one-time events
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                DetailTransactionEvent.NavigateBack -> {
                    navController.navigateUp()
                }

                is DetailTransactionEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                DetailTransactionEvent.TransactionDeleted -> {
                    Toast.makeText(context, context.getString(R.string.detail_transaction_deleted), Toast.LENGTH_SHORT).show()
                }

                DetailTransactionEvent.TransactionCopied -> {
                    Toast.makeText(context, context.getString(R.string.detail_transaction_copied), Toast.LENGTH_SHORT).show()
                }

                is DetailTransactionEvent.NavigateToEditTransaction -> {
                    navController.navigate(Screen.AddTransaction.createRoute(event.transactionId))
                }
                
                is DetailTransactionEvent.OpenImage -> {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            event.imageFile
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "image/png")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.detail_transaction_error_open_image), Toast.LENGTH_SHORT).show()
                    }
                }
                
                is DetailTransactionEvent.ShareImage -> {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            event.imageFile
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.detail_transaction_share_transaction)))
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.detail_transaction_error_share_image), Toast.LENGTH_SHORT).show()
                    }
                }

                null -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            DetailTransactionTopBar(
                onCloseClick = { navController.popBackStack() },
                onCopyClick = { viewModel.processIntent(DetailTransactionIntent.CopyTransaction) },
                onShareClick = { 
                    coroutineScope.launch {
                        // Directly trigger share dialog - bitmap will be rendered inside the dialog
                        viewModel.processIntent(DetailTransactionIntent.ShareTransaction)
                    }
                },
                onEditClick = { viewModel.processIntent(DetailTransactionIntent.EditTransaction) },
                onDeleteClick = { viewModel.processIntent(DetailTransactionIntent.DeleteTransaction) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppColor.Light.PrimaryColor.containerColor
    ) { paddingValues ->
        DetailTransactionContent(
            state = state,
            paddingValues = paddingValues,
            onConfirmDelete = { viewModel.processIntent(DetailTransactionIntent.ConfirmDeleteTransaction) },
            onDismissDialog = { viewModel.processIntent(DetailTransactionIntent.DismissDeleteConfirmation) }
        )
    }
    
    // Share image dialog
    if (state.showShareDialog && state.transactionImageFile != null && state.transaction != null) {
        ShareTransactionDialog(
            transaction = state.transaction!!,
            imageFile = state.transactionImageFile!!,
            onDismiss = { viewModel.processIntent(DetailTransactionIntent.DismissShareDialog) },
            onOpen = { viewModel.processIntent(DetailTransactionIntent.OpenTransactionImage) },
            onShare = { viewModel.processIntent(DetailTransactionIntent.ShareTransactionImage) }
        )
    }
}

@Composable
fun ShareTransactionDialog(
    transaction: Transaction,
    imageFile: File,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Render the transaction to bitmap when dialog is shown
    LaunchedEffect(transaction) {
        bitmap = renderTransactionToBitmap(transaction, context)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(AppColor.Light.PrimaryColor.cardColor)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_transaction_preview),
                    color = TextMain,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Image preview
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.detail_transaction_image_cd),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    CircularProgressIndicator()
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                if (bitmap != null) {
                                    try {
                                        // Save bitmap to file
                                        withContext(Dispatchers.IO) {
                                            saveBitmapToFile(bitmap!!, imageFile)
                                        }
                                        onOpen()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.detail_transaction_error_prepare_image), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF30D158)
                        )
                    ) {
                        Text(stringResource(R.string.detail_transaction_open_button))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (bitmap != null) {
                                    try {
                                        // Save bitmap to file
                                        withContext(Dispatchers.IO) {
                                            saveBitmapToFile(bitmap!!, imageFile)
                                        }
                                        onShare()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.detail_transaction_error_prepare_image_sharing), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF30D158)
                        )
                    ) {
                        Text(stringResource(R.string.detail_transaction_share_button))
                    }
                }
            }
        }
    }
}

// Helper function to save bitmap to file
private suspend fun saveBitmapToFile(bitmap: Bitmap, file: File) {
    withContext(Dispatchers.IO) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            android.util.Log.e("DetailTransactionScreen", "Error saving bitmap to file", e)
            throw e
        }
    }
}

// Function to render transaction to bitmap
private suspend fun renderTransactionToBitmap(transaction: Transaction?, context: android.content.Context): Bitmap? {
    if (transaction == null) return null
    
    return withContext(Dispatchers.Default) {
        try {
            // Create a blank bitmap with the desired dimensions
            val width = 1080
            val height = 1920 // Approximate height, will be adjusted as needed
            
            // Create a blank bitmap with transparent background
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            // Create a Canvas to draw on the bitmap
            val canvas = android.graphics.Canvas(bitmap)
            
            // Set background color
            canvas.drawColor(android.graphics.Color.parseColor("#FFFFFF"))
            
            // Draw transaction information directly onto the canvas
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 60f  // Heading text size
                color = android.graphics.Color.parseColor("#1E2A36") // TextMain color
            }
            
            // Draw transaction title (category)
            canvas.drawText(context.getString(getStringResId(context, transaction.category.title)), 50f, 150f, paint)
            
            // Draw amount with appropriate color
            paint.textSize = 80f
            paint.color = if (transaction.transactionType == TransactionType.OUTFLOW) 
                android.graphics.Color.parseColor("#FF453A") // Red for outflow
            else 
                android.graphics.Color.parseColor("#30D158") // Green for inflow
                
            // Use the new formatting utility
            val amount = formatAmountWithCurrency(
                transaction.amount, 
                transaction.wallet.currency.symbol
            )
            canvas.drawText(amount, 50f, 300f, paint)
            
            // Reset color and size for details
            paint.color = android.graphics.Color.parseColor("#1E2A36") // TextMain color
            paint.textSize = 50f
            
            // Draw description if available
            var yPos = 400f
            if (!transaction.description.isNullOrEmpty()) {
                canvas.drawText("${context.getString(R.string.detail_transaction_description_label)} ${transaction.description}", 50f, yPos, paint)
                yPos += 100f
            }
            
            // Draw date
            val dateFormat = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault())
            canvas.drawText("${context.getString(R.string.detail_transaction_date_label)} ${dateFormat.format(transaction.transactionDate)}", 50f, yPos, paint)
            yPos += 100f
            
            // Draw wallet name
            canvas.drawText("${context.getString(R.string.detail_transaction_wallet_label)} ${transaction.wallet.walletName}", 50f, yPos, paint)
            yPos += 100f
            
            // Draw people involved
            if (!transaction.withPerson.isNullOrEmpty()) {
                canvas.drawText("${context.getString(R.string.detail_transaction_with_label_text)} ${transaction.withPerson}", 50f, yPos, paint)
                yPos += 100f
            }
            
            // Draw event name if available
            if (!transaction.eventName.isNullOrEmpty()) {
                canvas.drawText("${context.getString(R.string.detail_transaction_event_label)} ${transaction.eventName}", 50f, yPos, paint)
                yPos += 100f
            }
            
            // App watermark
            paint.textSize = 40f
            paint.color = android.graphics.Color.GRAY
            canvas.drawText(context.getString(R.string.detail_transaction_created_with), 50f, yPos + 100f, paint)
            
            // Trim the bitmap to the actual content height
            val trimmedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, (yPos + 200).toInt())
            
            trimmedBitmap
        } catch (e: Exception) {
            android.util.Log.e("DetailTransactionScreen", "Error rendering transaction to bitmap", e)
            null
        }
    }
}

@Composable
fun DetailTransactionContent(
    state: DetailTransactionState,
    paddingValues: PaddingValues,
    onConfirmDelete: () -> Unit,
    onDismissDialog: () -> Unit
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (state.transaction != null) {
        val transaction = state.transaction!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Hiển thị ảnh giao dịch nếu có
            if (!transaction.photoUri.isNullOrEmpty()) {
                AsyncImage(
                    model = transaction.photoUri,
                    contentDescription = "Transaction Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .padding(bottom = 16.dp)
                )
            }

            // Thông tin chính
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painterResource(id = getDrawableResId(LocalContext.current, transaction.category.icon)),
                                contentDescription = null,
                            )
                        }
                        Text(
                            text = stringResource(getStringResId(LocalContext.current, transaction.category.title)),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextMain,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                    Text(
                        text = formatAmountWithCurrency(transaction.amount, transaction.wallet.currency.symbol),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (transaction.transactionType == TransactionType.OUTFLOW) Color(0xFFFF453A) else Color(0xFF30D158),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            // Chi tiết giao dịch
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (!transaction.description.isNullOrEmpty()) {
                        TransactionDetailItem(
                            icon = painterResource(id = R.drawable.ic_description),
                            content = {
                                Text(
                                    text = transaction.description,
                                    color = TextMain,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                    TransactionDetailItem(
                        icon = painterResource(id = R.drawable.ic_calendar),
                        content = {
                            val dateFormat = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault())
                            Text(
                                text = dateFormat.format(transaction.transactionDate),
                                color = TextMain
                            )
                        }
                    )
                    TransactionDetailItem(
                        icon = painterResource(id = R.drawable.ic_wallet),
                        content = {
                            Text(
                                text = transaction.wallet.walletName,
                                color = TextMain
                            )
                        }
                    )
                }
            }

            // Card riêng cho phần With
            if (state.listContacts.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_with),
                                contentDescription = null,
                                tint = TextMain,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.detail_transaction_with_label),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.listContacts.forEach { contact ->
                                ContactChip(contact = contact, onRemove = {})
                            }
                        }
                    }
                }
            }
        }
    } else if (state.error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.detail_transaction_error_loading),
                color = TextMain
            )
        }
    }
    
    // Delete confirmation dialog
    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { 
                Text(
                    stringResource(R.string.detail_transaction_delete_title),
                    color = TextMain,
                    style = MaterialTheme.typography.titleLarge
                ) 
            },
            text = { 
                Text(
                    stringResource(R.string.detail_transaction_delete_message),
                    color = TextMain
                ) 
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A))
                ) {
                    Text(stringResource(R.string.detail_transaction_delete_button), color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissDialog) {
                    Text("Cancel", color = TextMain)
                }
            },
            containerColor = AppColor.Light.PrimaryColor.cardColor
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6F6)
@Composable
fun DetailTransactionContentPreview() {
    val mockTransaction = Transaction(
        id = 1,
        wallet = Wallet(
            id = 1,
            walletName = "Cash",
            currentBalance = 1000000.0,
            currency = Currency(
                id = 1,
                currencyName = "Vietnamese Dong",
                currencyCode = "VND",
                symbol = "₫"
            )
        ),
        amount = 250000.0,
        transactionType = TransactionType.OUTFLOW,
        category = Category(
            id = 1,
            metaData = "food_drinks",
            title = "Food & Drinks",
            icon = "food_icon",
            type = CategoryType.EXPENSE
        ),
        description = "Dinner with friends",
        transactionDate = Date(),
        withPerson = "[{\"id\":\"1\",\"name\":\"John Doe\",\"initial\":\"J\"},{\"id\":\"2\",\"name\":\"Mary Smith\",\"initial\":\"M\"}]"
    )
    
    val mockContacts = listOf(
        Contact(
            id = "1",
            name = "John Doe",
            initial = 'J'
        ),
        Contact(
            id = "2", 
            name = "Mary Smith",
            initial = 'M'
        )
    )
    
    DetailTransactionContent(
        state = DetailTransactionState(
            transaction = mockTransaction,
            isLoading = false,
            listContacts = mockContacts
        ),
        paddingValues = PaddingValues(),
        onConfirmDelete = {},
        onDismissDialog = {}
    )
}

