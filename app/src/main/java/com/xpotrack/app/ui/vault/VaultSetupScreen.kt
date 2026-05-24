package com.xpotrack.app.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.xpotrack.app.R
import com.xpotrack.app.data.security.VaultKeyStore
import com.xpotrack.app.ui.components.XpPrimaryButton
import com.xpotrack.app.ui.components.cutoutSafeTopPadding
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun VaultSetupScreen(vm: VaultViewModel) {
    val activity = LocalContext.current as FragmentActivity
    val biometricAvailable = remember { VaultBiometric.isAvailable(activity) }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var enableBio by remember { mutableStateOf(biometricAvailable) }
    var error by remember { mutableStateOf<String?>(null) }

    val submit: () -> Unit = submit@{
        error = when {
            pass.length < 6 -> "Use at least 6 characters"
            pass != confirm -> "Passphrases don't match"
            else -> null
        }
        if (error != null) return@submit
        if (enableBio && biometricAvailable) {
            val cipher = runCatching { VaultKeyStore.initEncryptCipher() }.getOrNull()
            if (cipher == null) {
                vm.setupPassphrase(pass.toCharArray(), enableBiometric = false)
                return@submit
            }
            VaultBiometric.prompt(
                activity = activity, cipher = cipher,
                title = "Enable fingerprint unlock",
                subtitle = "Future opens won't need the passphrase.",
                onSuccess = { authed -> vm.setupPassphrase(pass.toCharArray(), enableBiometric = true, biometricCipher = authed) },
                onError = { msg -> error = msg },
                onCancel = { vm.setupPassphrase(pass.toCharArray(), enableBiometric = false) },
            )
        } else {
            vm.setupPassphrase(pass.toCharArray(), enableBiometric = false)
        }
    }

    Column(
        Modifier.fillMaxSize().background(XpTokens.Bg).cutoutSafeTopPadding().imePadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_lock), null, tint = XpTokens.TealDim, modifier = Modifier.size(11.dp))
            Spacer(Modifier.size(6.dp))
            Text("VAULT · SETUP", style = MaterialTheme.typography.labelSmall, color = XpTokens.TealDim)
        }

        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Create a passphrase",
                color = XpTokens.Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "It encrypts every vault note on this device. There is no recovery if you forget it.",
                color = XpTokens.Ink2, fontSize = 13.5.sp, lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            PassField("Passphrase", pass, { pass = it })
            Spacer(Modifier.height(14.dp))
            PassField("Confirm", confirm, { confirm = it })

            if (biometricAvailable) {
                Spacer(Modifier.height(20.dp))
                BiometricToggle(enableBio) { enableBio = it }
            }

            error?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = XpTokens.Alarm, fontSize = 12.sp)
            }

            Spacer(Modifier.height(24.dp))
            XpPrimaryButton("Create vault", enabled = pass.isNotEmpty() && confirm.isNotEmpty(), onClick = submit)
        }
    }
}

@Composable
private fun PassField(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = XpTokens.Ink3)
        Spacer(Modifier.height(8.dp))
        PassInput(value, onChange)
    }
}

@Composable
private fun BiometricToggle(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(XpTokens.Surface1).border(0.5.dp, XpTokens.Hair, RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(!enabled) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_fingerprint), null, tint = XpTokens.TealDim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Enable fingerprint unlock", color = XpTokens.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Skip the passphrase on future opens", color = XpTokens.Ink3, fontSize = 12.sp)
        }
        Box(
            Modifier.size(width = 36.dp, height = 20.dp).clip(CircleShape)
                .background(if (enabled) XpTokens.Teal else XpTokens.Surface2),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.padding(horizontal = 2.dp).size(16.dp).clip(CircleShape)
                .background(if (enabled) XpTokens.OnTeal else XpTokens.Ink3))
        }
    }
}

