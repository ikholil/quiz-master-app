package app.prostuti.feature.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.prostuti.core.designsystem.component.PtButton
import app.prostuti.core.designsystem.component.PtButtonStyle
import app.prostuti.core.designsystem.component.PtTextField
import app.prostuti.core.designsystem.component.SegmentedControl
import app.prostuti.core.designsystem.theme.Spacing
import app.prostuti.core.ui.errorMessage

@Composable
internal fun EmailScreen(onBack: () -> Unit, viewModel: EmailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPassword by rememberSaveable { mutableStateOf(false) }
    AuthStep(
        stringResource(if (state.register) R.string.auth_register_title else R.string.auth_email_title),
        stringResource(R.string.auth_tagline),
        onBack,
    ) {
        SegmentedControl(
            options = listOf(false, true),
            selected = state.register,
            onSelect = viewModel::setRegister,
            label = { stringResource(if (it) R.string.auth_tab_register else R.string.auth_tab_sign_in) },
        )
        Spacer(Modifier.height(Spacing.xl))
        if (state.register) {
            PtTextField(
                state.name, viewModel::onName, stringResource(R.string.auth_name), leadingIcon = Icons.Rounded.Person,
                error = if (state.showValidation && !state.nameValid) stringResource(R.string.auth_name_required) else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(Spacing.md))
        }
        PtTextField(
            state.email, viewModel::onEmail, stringResource(R.string.auth_email), leadingIcon = Icons.Rounded.Email,
            error = if (state.showValidation && !state.emailValid) stringResource(R.string.auth_email_invalid) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(Spacing.md))
        PtTextField(
            state.password, viewModel::onPassword, stringResource(R.string.auth_password), leadingIcon = Icons.Rounded.Lock,
            supportingText = stringResource(R.string.auth_password_hint),
            error = when {
                state.error != null -> errorMessage(state.error!!)
                state.showValidation && !state.passwordValid -> stringResource(R.string.auth_password_hint)
                else -> null
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
            trailing = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        stringResource(if (showPassword) R.string.auth_hide_password else R.string.auth_show_password),
                    )
                }
            },
        )
        Spacer(Modifier.height(Spacing.xl))
        PtButton(
            stringResource(if (state.register) R.string.auth_create else R.string.auth_sign_in),
            viewModel::submit, Modifier.fillMaxWidth(), loading = state.loading,
        )
        if (!state.register) {
            PtButton(stringResource(R.string.auth_forgot), viewModel::forgot, Modifier.fillMaxWidth(), style = PtButtonStyle.Text)
        }
        state.resetSentTo?.let {
            Text(stringResource(R.string.auth_reset_sent, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
