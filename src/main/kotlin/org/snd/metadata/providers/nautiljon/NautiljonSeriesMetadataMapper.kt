package org.snd.metadata.providers.nautiljon

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.*
import org.snd.metadata.providers.nautiljon.model.Series
import org.snd.metadata.providers.nautiljon.model.Volume

class NautiljonSeriesMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
) {
    private val artistRoles = listOf(
        AuthorRole.PENCILLER,
        AuthorRole.INKER,
        AuthorRole.COLORIST,
        AuthorRole.LETTERER,
        AuthorRole.COVER
    )

    fun toSeriesMetadata(series: Series, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
            "En cours" -> SeriesStatus.ONGOING
            "En attente" -> SeriesStatus.ONGOING
            "Terminé" -> SeriesStatus.ENDED
            else -> SeriesStatus.ONGOING
        }


        val authors = series.authorsStory.map { Author(it, AuthorRole.WRITER) } +
                series.authorsArt.flatMap { artist -> artistRoles.map { role -> Author(artist, role) } }

        val tags = series.themes + listOfNotNull(
            seriesMetadataConfig.originalPublisherTagName
                ?.let { tag -> series.originalPublisher?.let { publisher -> "$tag: $publisher" } },
            seriesMetadataConfig.frenchPublisherTagName
                ?.let { tag -> series.frenchPublisher?.let { publisher -> "$tag: $publisher" } }
        )

        val metadata = SeriesMetadata(
            status = status,
            title = series.title,
            titleSort = series.title,
            summary = series.description,
            publisher = if (seriesMetadataConfig.useOriginalPublisher) series.originalPublisher
            else series.frenchPublisher ?: series.originalPublisher,
            alternativePublishers = if (seriesMetadataConfig.useOriginalPublisher) listOfNotNull(series.frenchPublisher)
            else series.frenchPublisher?.let { listOfNotNull(series.originalPublisher) } ?: emptyList(),
            genres = series.genres,
            tags = tags,
            authors = authors,
            thumbnail = thumbnail,
            totalBookCount = series.numberOfVolumes,
            ageRating = series.recommendedAge,
            alternativeTitles = series.alternativeTitles + series.originalTitles
        )
        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(series.id.id),
            metadata = metadata,
            books = series.volumes.map {
                SeriesBook(
                    id = ProviderBookId(it.id.id),
                    number = it.number,
                    edition = it.edition,
                    type = it.type,
                    name = it.name
                )
            }
        )
        return MetadataConfigApplier.apply(providerMetadata, seriesMetadataConfig)
    }

    fun toBookMetadata(volume: Volume, thumbnail: Image? = null): ProviderBookMetadata {
        val authors = volume.authorsStory.map { Author(it, AuthorRole.WRITER) } +
                volume.authorsArt.flatMap { artist -> artistRoles.map { role -> Author(artist, role) } }

        val metadata = BookMetadata(
            summary = volume.description,
            number = volume.number,
            releaseDate = if (seriesMetadataConfig.useOriginalPublisher) volume.originalReleaseDate else volume.frenchReleaseDate,
            authors = authors,
            startChapter = null,
            endChapter = null,

            thumbnail = thumbnail
        )
        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(volume.id.id),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }
}