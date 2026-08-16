package com.confused.onlylist.network.anilist

/**
 * AniList GraphQL queries.
 * Per R-2: fields verified against the live AniList GraphQL API.
 */
object AniListQueries {

    const val MEDIA_FIELDS = """
        id
        idMal
        type
        title { romaji english native }
        coverImage { large extraLarge color }
        bannerImage
        episodes
        chapters
        duration
        status
        season
        seasonYear
        format
        source
        averageScore
        meanScore
        popularity
        favourites
        genres
        description(asHtml: false)
        nextAiringEpisode { airingAt episode timeUntilAiring }
        updatedAt
    """

    val trending = """
        query Trending(${'$'}page: Int, ${'$'}perPage: Int, ${'$'}type: MediaType) {
            Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo { hasNextPage }
                media(type: ${'$'}type, sort: TRENDING_DESC) {
                    $MEDIA_FIELDS
                }
            }
        }
    """.trimIndent()

    val search = """
        query Search(${'$'}page: Int, ${'$'}perPage: Int, ${'$'}search: String, ${'$'}type: MediaType) {
            Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo { hasNextPage }
                media(type: ${'$'}type, search: ${'$'}search, sort: POPULARITY_DESC) {
                    $MEDIA_FIELDS
                }
            }
        }
    """.trimIndent()

    val mediaById = """
        query MediaById(${'$'}id: Int) {
            Media(id: ${'$'}id) {
                $MEDIA_FIELDS
            }
        }
    """.trimIndent()

    val viewer = """
        query Viewer {
            Viewer {
                id
                name
                avatar { large medium }
                bannerImage
                options { displayAdultContent }
                mediaListOptions { scoreFormat }
            }
        }
    """.trimIndent()
}
