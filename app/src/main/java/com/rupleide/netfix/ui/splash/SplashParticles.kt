package com.rupleide.netfix.ui.splash

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class DisintegrationParticle(
    val relX: Float,
    val relY: Float,
    val baseColor: Color,
    val baseSize: Float,
    val travelAngle: Float,
    val travelDistance: Float,
    val startDelay: Float,
    val driftPhase: Float,
    val driftFrequency: Float,
    val driftAmplitude: Float,
    val spinDirection: Float,
    val flicker: Float
)

class SplashParticleField private constructor(
    val particles: List<DisintegrationParticle>,
    val sourceRadiusPx: Float
) {
    companion object {
        fun sample(
            context: Context,
            @DrawableRes drawableRes: Int,
            gridSize: Int = 34,
            maxParticles: Int = 190,
            targetRadiusPx: Float,
            primaryColor: Color,
            secondaryColor: Color,
            seed: Random = Random(System.nanoTime())
        ): SplashParticleField {
            val drawable = ContextCompat.getDrawable(context, drawableRes)
            val bitmap: Bitmap = drawable?.toBitmap(gridSize, gridSize)
                ?: Bitmap.createBitmap(gridSize, gridSize, Bitmap.Config.ARGB_8888)

            val half = gridSize / 2f
            val candidates = ArrayList<DisintegrationParticle>(gridSize * gridSize / 2)

            for (py in 0 until gridSize) {
                for (px in 0 until gridSize) {
                    val argb = bitmap.getPixel(px, py)
                    val alpha = (argb ushr 24) and 0xFF
                    if (alpha < 36) continue

                    val nx = (px + 0.5f - half) / half
                    val ny = (py + 0.5f - half) / half
                    val dist = sqrt(nx * nx + ny * ny)
                    if (dist > 1.06f) continue

                    val srcR = ((argb ushr 16) and 0xFF) / 255f
                    val srcG = ((argb ushr 8) and 0xFF) / 255f
                    val srcB = (argb and 0xFF) / 255f
                    val srcColor = Color(srcR, srcG, srcB, alpha / 255f)

                    val brandTint = lerp(Color(0xFFA1A1AA), Color(0xFFF4F4F5), seed.nextFloat())
                    val finalColor = lerp(srcColor, brandTint, 0.7f).copy(alpha = 1f)

                    val jitter = (seed.nextFloat() - 0.5f) * 0.5f
                    val baseAngle = atan2(ny, nx) + jitter
                    val outwardBoost = 0.55f + dist * 0.65f
                    val magnitude = (targetRadiusPx * (1.5f + outwardBoost)) * (0.65f + seed.nextFloat() * 0.7f)

                    val verticalFactor = ((ny + 1f) / 2f).coerceIn(0f, 1f)
                    val radialFactor = dist.coerceIn(0f, 1f)
                    val sweepDelay = verticalFactor * 0.36f + radialFactor * 0.1f
                    val randomDelay = seed.nextFloat() * 0.2f
                    val startDelay = (sweepDelay + randomDelay).coerceIn(0f, 0.6f)

                    candidates += DisintegrationParticle(
                        relX = nx,
                        relY = ny,
                        baseColor = finalColor,
                        baseSize = 1.6f + seed.nextFloat() * 2.4f,
                        travelAngle = baseAngle,
                        travelDistance = magnitude,
                        startDelay = startDelay,
                        driftPhase = seed.nextFloat() * (2f * Math.PI.toFloat()),
                        driftFrequency = 1.2f + seed.nextFloat() * 1.8f,
                        driftAmplitude = targetRadiusPx * (0.05f + seed.nextFloat() * 0.09f),
                        spinDirection = if (seed.nextBoolean()) 1f else -1f,
                        flicker = seed.nextFloat()
                    )
                }
            }

            val trimmed = if (candidates.size > maxParticles) {
                candidates.shuffled(seed).take(maxParticles)
            } else candidates

            return SplashParticleField(trimmed, targetRadiusPx)
        }
    }
}

data class ParticleFrame(
    val position: Offset,
    val trailPosition: Offset,
    val color: Color,
    val coreRadius: Float,
    val haloRadius: Float,
    val alpha: Float
)

fun computeParticleFrame(
    particle: DisintegrationParticle,
    origin: Offset,
    globalProgress: Float
): ParticleFrame? {
    val local = ((globalProgress - particle.startDelay) / (1f - particle.startDelay))
        .coerceIn(0f, 1f)
    if (local <= 0f) return null

    val inv = 1f - local
    val eased = 1f - inv * inv * inv * inv
    val trailLocal = (local - 0.03f).coerceIn(0f, 1f)
    val trailInv = 1f - trailLocal
    val trailEased = 1f - trailInv * trailInv * trailInv * trailInv

    val travel = particle.travelDistance * eased
    val trailTravel = particle.travelDistance * trailEased

    val driftAngle = particle.driftPhase + local * particle.driftFrequency * 6.283185f
    val drift = sin(driftAngle) * particle.driftAmplitude * local * particle.spinDirection
    val gravityPull = local * local * particle.baseSize * 7f

    val dirX = cos(particle.travelAngle)
    val dirY = sin(particle.travelAngle)
    val driftX = cos(particle.driftPhase) * drift
    val driftY = sin(particle.driftPhase) * drift

    val position = Offset(
        origin.x + particle.relX * 4f + dirX * travel + driftX,
        origin.y + particle.relY * 4f + dirY * travel + driftY + gravityPull
    )
    val trailPosition = Offset(
        origin.x + particle.relX * 4f + dirX * trailTravel + driftX * 0.9f,
        origin.y + particle.relY * 4f + dirY * trailTravel + driftY * 0.9f + gravityPull * 0.85f
    )

    val fade = 1f - local
    val alpha = (fade * (2f - fade)).coerceIn(0f, 1f)
    val size = particle.baseSize * (1f - local * 0.35f)

    return ParticleFrame(
        position = position,
        trailPosition = trailPosition,
        color = particle.baseColor,
        coreRadius = max(0.4f, size),
        haloRadius = size * 2f,
        alpha = alpha
    )
}
