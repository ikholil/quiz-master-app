package app.prostuti.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.core.designsystem.component.PtButton
import app.prostuti.core.designsystem.component.PtButtonStyle
import app.prostuti.core.designsystem.component.PtScaffold
import app.prostuti.core.designsystem.component.PtTextField
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.designsystem.theme.tabular
import app.prostuti.core.model.OtpChallenge
import app.prostuti.core.ui.LocalBuildInfo
import app.prostuti.core.ui.errorMessage
import app.prostuti.core.ui.loc

@Composable
internal fun AuthStep(title: String, subtitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    PtScaffold(title = "", onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(horizontal = Spacing.xxl)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(Spacing.sm))
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xxl))
                content()
            }
        }
    }
}

@Composable
internal fun PhoneScreen(
    onBack: () -> Unit,
    onCodeSent: (OtpChallenge) -> Unit,
    viewModel: PhoneViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.sent.collect(onCodeSent) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AuthStep(stringResource(R.string.auth_phone_title), stringResource(R.string.auth_phone_subtitle), onBack) {
        PtTextField(
            value = state.phone,
            onValueChange = viewModel::onPhoneChange,
            label = stringResource(R.string.auth_phone_label),
            placeholder = "01XXXXXXXXX",
            leadingIcon = Icons.Rounded.Smartphone,
            error = when {
                state.error != null -> errorMessage(state.error!!)
                state.showValidation && !state.isValid -> stringResource(R.string.auth_phone_invalid)
                else -> null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
            modifier = Modifier.focusRequester(focus),
        )
        Spacer(Modifier.height(Spacing.xl))
        PtButton(stringResource(R.string.auth_send_code), viewModel::submit, Modifier.fillMaxWidth(), loading = state.loading)
    }
}

@Composable
internal fun OtpScreen(onBack: () -> Unit, viewModel: OtpViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AuthStep(stringResource(R.string.auth_otp_title), stringResource(R.string.auth_otp_subtitle, state.phone.loc()), onBack) {
        val description = stringResource(R.string.auth_otp_field, state.codeLength)
        BasicTextField(
            value = state.code,
            onValueChange = viewModel::onCodeChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.verify() }),
            modifier = Modifier.focusRequester(focus).semantics { contentDescription = description },
            decorationBox = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(state.codeLength) { i ->
                        val char = state.code.getOrNull(i)?.toString().orEmpty()
                        val active = i == state.code.length
                        val color = when {
                            state.error != null -> MaterialTheme.colorScheme.error
                            active -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                        Box(
                            Modifier.weight(1f).height(56.dp).border(BorderStroke(if (active) 2.dp else 1.dp, color), MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(char.loc(), style = MaterialTheme.typography.headlineSmall.tabular, textAlign = TextAlign.Center)
                        }
                    }
                }
            },
        )
        state.error?.let {
            Spacer(Modifier.height(Spacing.sm))
            Text(errorMessage(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (LocalBuildInfo.current.isMockBackend) {
            Spacer(Modifier.height(Spacing.sm))
            Text(stringResource(R.string.auth_dev_otp, "123456"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        }
        Spacer(Modifier.height(Spacing.xl))
        PtButton(stringResource(R.string.auth_verify), viewModel::verify, Modifier.fillMaxWidth(), loading = state.verifying, enabled = state.code.length == state.codeLength)
        Spacer(Modifier.height(Spacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            PtButton(stringResource(R.string.auth_change_number), onBack, style = PtButtonStyle.Text)
            if (state.secondsToResend > 0) {
                Text(stringResource(R.string.auth_resend_in, state.secondsToResend.loc()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                PtButton(stringResource(R.string.auth_resend), { viewModel.resend() }, style = PtButtonStyle.Text)
            }
        }
    }
}

@Suppress("unused") private val s = Modifier.size(0.dp)
