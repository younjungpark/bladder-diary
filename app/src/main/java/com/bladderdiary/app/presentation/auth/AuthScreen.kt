package com.bladderdiary.app.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bladderdiary.app.domain.model.SocialProvider

@Composable
fun AuthScreen(
    viewModel: AuthViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
    val secondaryGlow = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f)
    val tertiaryGlow = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                )
            )
            .drawBehind {
                drawCircle(
                    color = primaryGlow,
                    radius = size.minDimension * 0.35f,
                    center = Offset(size.width * 0.1f, size.height * 0.18f)
                )
                drawCircle(
                    color = secondaryGlow,
                    radius = size.minDimension * 0.28f,
                    center = Offset(size.width * 0.9f, size.height * 0.14f)
                )
                drawCircle(
                    color = tertiaryGlow,
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.55f, size.height * 1.05f)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(999.dp),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = "BLADDER DIARY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "하루의 배뇨 기록을\n차분하게 이어가세요",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "소셜 계정으로 바로 시작하고, 중요한 기록은 같은 톤의 화면 안에서 빠르게 확인할 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "소셜 로그인",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Google 또는 카카오 계정으로 연결하면 새 기기에서도 기록을 이어서 볼 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SocialLoginButton(
                        text = if (state.pendingProvider == SocialProvider.GOOGLE && state.isOAuthLoading) {
                            "Google 로그인 진행 중"
                        } else {
                            "Google 계정으로 계속"
                        },
                        icon = Icons.Default.AccountCircle,
                        backgroundColor = Color.White,
                        contentColor = Color(0xFF171D1D),
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                        loading = state.pendingProvider == SocialProvider.GOOGLE && state.isOAuthLoading,
                        enabled = !state.isOAuthLoading,
                        onClick = { viewModel.signInWithSocial(SocialProvider.GOOGLE) }
                    )

                    SocialLoginButton(
                        text = if (state.pendingProvider == SocialProvider.KAKAO && state.isOAuthLoading) {
                            "카카오 로그인 진행 중"
                        } else {
                            "카카오 계정으로 계속"
                        },
                        icon = Icons.Default.Person,
                        backgroundColor = Color(0xFFFEE500),
                        contentColor = Color(0xFF171D1D),
                        loading = state.pendingProvider == SocialProvider.KAKAO && state.isOAuthLoading,
                        enabled = !state.isOAuthLoading,
                        onClick = { viewModel.signInWithSocial(SocialProvider.KAKAO) }
                    )

                    if (state.isOAuthLoading) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = "브라우저 인증 후 앱으로 다시 돌아오면 자동으로 이어집니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialLoginButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.72f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = borderColor?.let { BorderStroke(1.dp, it) },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = contentColor.copy(alpha = 0.08f),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = contentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
