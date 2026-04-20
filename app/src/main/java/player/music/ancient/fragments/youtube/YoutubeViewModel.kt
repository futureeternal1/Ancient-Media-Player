package player.music.ancient.fragments.youtube

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import player.music.ancient.db.YoutubeChannelEntity
import player.music.ancient.repository.Repository

class YoutubeViewModel(private val repository: Repository) : ViewModel() {
    val youtubeChannels: LiveData<List<YoutubeChannelEntity>> = repository.getAllYoutubeChannels()

    fun insertYoutubeChannel(channel: YoutubeChannelEntity) {
        viewModelScope.launch(IO) {
            repository.insertYoutubeChannel(channel)
        }
    }

    fun updateYoutubeChannel(channel: YoutubeChannelEntity) {
        viewModelScope.launch(IO) {
            repository.updateYoutubeChannel(channel)
        }
    }

    fun deleteYoutubeChannel(channel: YoutubeChannelEntity) {
        viewModelScope.launch(IO) {
            repository.deleteYoutubeChannel(channel)
        }
    }
}
