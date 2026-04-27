package player.music.ancient.extensions

import org.junit.Test
import org.junit.Assert.assertEquals
import player.music.ancient.model.Song
import java.io.File
import org.junit.After
import org.junit.Before

class FragmentMusicExtensionsTest {

    private lateinit var tempInvalidFile: File

    @Before
    fun setUp() {
        // Create a temporary file that is not a valid audio file to trigger the catch block
        tempInvalidFile = File.createTempFile("invalid_audio", ".mp3")
        tempInvalidFile.writeText("This is not a valid audio file.")
    }

    @After
    fun tearDown() {
        if (::tempInvalidFile.isInitialized && tempInvalidFile.exists()) {
            tempInvalidFile.delete()
        }
    }

    @Test
    fun testGetSongInfoFileNotExists() {
        val song = Song(
            id = -1,
            title = "",
            trackNumber = -1,
            year = -1,
            duration = -1,
            data = "non_existent_file.mp3",
            dateModified = -1,
            albumId = -1,
            albumName = "",
            artistId = -1,
            artistName = "",
            composer = "",
            albumArtist = ""
        )

        // File doesn't exist, so it should return "-"
        val info = getSongInfo(song)
        assertEquals("-", info)
    }

    @Test
    fun testGetSongInfoExceptionPath() {
        val song = Song(
            id = -1,
            title = "",
            trackNumber = -1,
            year = -1,
            duration = -1,
            data = tempInvalidFile.absolutePath,
            dateModified = -1,
            albumId = -1,
            albumName = "",
            artistId = -1,
            artistName = "",
            composer = "",
            albumArtist = ""
        )

        // File exists, but is invalid, triggering AudioFileIO exception
        // Expecting " - " as per catch block logic
        val info = getSongInfo(song)
        assertEquals(" - ", info)
    }
}
