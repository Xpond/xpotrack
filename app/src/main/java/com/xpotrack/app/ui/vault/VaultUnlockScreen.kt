package com.xpotrack.app.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xpotrack.app.R
import com.xpotrack.app.data.security.VaultKeyStore
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun VaultUnlockScreen(vm: VaultViewModel) {
    val activity = LocalContext.current as FragmentActivity
    val biometricAvailable = remember { VaultBiometric.isAvailable(activity) }
    val hasBiometricBlob = remember { vm.hasBiometric() }
    val canBiometric = biometricAvailable && hasBiometricBlob

    var showPassphrase by remember { mutableStateOf(!canBiometric) }
    var pass by remember { mutableStateOf("") }
    val err by vm.unlockError.collectAsStateWithLifecycle()
    val verifying by vm.verifying.collectAsStateWithLifecycle()

    val triggerBiometric = trigger@{
        val cipher = runCatching { VaultKeyStore.initDecryptCipher(vm.biometricIv()) }
            .getOrNull() ?: run { showPassphrase = true; return@trigger }
        VaultBiometric.prompt(
            activity = activity, cipher = cipher,
            title = "Unlock vault", subtitle = "Touch the sensor to open your notes.",
            onSuccess = { authed -> vm.unlockWithBiometric(authed) },
            onError = { showPassphrase = true },
            onCancel = { showPassphrase = true },
        )
    }

    // Auto-trigger fingerprint on first composition if available.
    LaunchedEffect(Unit) { if (canBiometric) triggerBiometric() }

    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color(0xFF030B0A), 0.5f to Color(0xFF050D0C), 1f to XpTokens.Bg,
            ),
        ).padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(20.dp))

        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FingerprintBadge()
            Spacer(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_lock), null, tint = XpTokens.TealDim, modifier = Modifier.size(11.dp))
                Spacer(Modifier.size(6.dp))
                Text("VAULT LOCKED", style = MaterialTheme.typography.labelSmall, color = XpTokens.TealDim)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (showPassphrase) "Enter passphrase" else "Touch the sensor",
                color = XpTokens.Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Encrypted on this device — never synced.",
                color = XpTokens.Ink2, fontSize = 13.5.sp,
            )

            if (showPassphrase) {
                Spacer(Modifier.height(28.dp))
                PassphraseEntry(
                    pass, { pass = it },
                    verifying = verifying,
                    onSubmit = { vm.unlockWithPassphrase(pass.toCharArray()) },
                )
            }

            err?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = XpTokens.Alarm, fontSize = 12.sp)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        ) {
            if (!showPassphrase && canBiometric) {
                FallbackButton("Use passphrase instead") { showPassphrase = true }
            } else if (canBiometric) {
                FallbackButton("Use fingerprint") { showPassphrase = false; triggerBiometric() }
            }
        }
    }
}

@Composable
private fun FingerprintBadge() {
    Box(
        Modifier.size(152.dp).clip(CircleShape)
            .background(Brush.radialGradient(0f to Color(0x145EEAD4), 1f to Color.Transparent))
            .border(0.5.dp, XpTokens.Teal, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(116.dp).clip(CircleShape)
                .background(Color(0x0F5EEAD4))
                .border(0.5.dp, XpTokens.Hair2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_fingerprint), null, tint = XpTokens.Teal, modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun PassphraseEntry(value: String, onChange: (String) -> Unit, verifying: Boolean, onSubmit: () -> Unit) {
    val canSubmit = value.isNotEmpty() && !verifying
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                .background(XpTokens.Surface1).border(0.5.dp, XpTokens.Hair, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value, onValueChange = onChange,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = XpTokens.Ink, fontSize = 15.sp, fontFamily = com.xpotrack.app.ui.theme.GeistMono,
                ),
                cursorBrush = SolidColor(XpTokens.Teal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                .background(if (canSubmit) XpTokens.Teal else XpTokens.Surface2)
                .clickable(enabled = canSubmit, onClick = onSubmit),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (verifying) "Verifying…" else "Unlock",
                color = if (canSubmit) XpTokens.OnTeal else XpTokens.Ink3,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FallbackButton(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(CircleShape)
            .border(0.5.dp, XpTokens.Hair2, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp),
    ) {
        Text(label, color = XpTokens.Ink2, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
