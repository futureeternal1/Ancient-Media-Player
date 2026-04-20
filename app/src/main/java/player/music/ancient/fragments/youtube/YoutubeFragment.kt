package player.music.ancient.fragments.youtube

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import player.music.ancient.R
import player.music.ancient.common.ATHToolbarActivity
import player.music.ancient.databinding.DialogAddYoutubeChannelBinding
import player.music.ancient.databinding.FragmentYoutubeBinding
import player.music.ancient.db.YoutubeChannelEntity
import player.music.ancient.fragments.base.AbsMainActivityFragment
import player.music.ancient.util.PreferenceUtil
import player.music.ancient.util.RadioImageStore
import player.music.ancient.util.ToolbarContentTintHelper
import com.bumptech.glide.Glide

class YoutubeFragment : AbsMainActivityFragment(R.layout.fragment_youtube) {
    private val viewModel: YoutubeViewModel by viewModel()
    private var _binding: FragmentYoutubeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: YoutubeAdapter
    private var pendingImageCallback: ((String) -> Unit)? = null
    private var seedStarted = false

    private val artworkPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = pendingImageCallback ?: return@registerForActivityResult
            pendingImageCallback = null
            if (result.resultCode == Activity.RESULT_OK) {
                val pickedUri = result.data?.data ?: return@registerForActivityResult
                lifecycleScope.launch {
                    val savedImageUri = RadioImageStore.persistStationImage(requireContext(), pickedUri)
                    if (savedImageUri == null) {
                        Toast.makeText(requireContext(), "Couldn't save the selected image", Toast.LENGTH_SHORT).show()
                    } else {
                        callback(savedImageUri)
                    }
                }
            } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(requireContext(), ImagePicker.getError(result.data), Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentYoutubeBinding.bind(view)
        mainActivity.setSupportActionBar(binding.appBarLayout.toolbar)
        binding.appBarLayout.title = getString(R.string.youtube)

        binding.emptyState.iconView.setImageResource(R.drawable.ic_youtube)
        binding.emptyState.titleView.text = "No YouTube Channels"
        binding.emptyState.messageView.text = "Add Christian YouTube channels you want to watch from the app."

        adapter = YoutubeAdapter(
            onClick = ::openChannel,
            onLongClick = ::showEditOrDeleteDialog
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.addChannelFab.setOnClickListener { showChannelBottomSheet() }

        viewModel.youtubeChannels.observe(viewLifecycleOwner) { channels ->
            val currentChannels = channels.orEmpty()
            syncKnownChannels(currentChannels)
            adapter.submitList(currentChannels)
            binding.emptyState.root.visibility = if (currentChannels.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (currentChannels.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun syncKnownChannels(channels: List<YoutubeChannelEntity>) {
        if (PreferenceUtil.youtubeDefaultsInitialized) return

        val missingChannels = KNOWN_CHANNELS.filter { known ->
            channels.none { it.name.equals(known.name, ignoreCase = true) || it.url == known.url }
        }
        if (missingChannels.isEmpty()) {
            PreferenceUtil.youtubeDefaultsInitialized = true
            return
        }

        if (seedStarted) return

        seedStarted = true
        missingChannels.forEach { known ->
            viewModel.insertYoutubeChannel(known)
        }
        PreferenceUtil.youtubeDefaultsInitialized = true
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_layout_type)
        menu.removeItem(R.id.action_sort_order)
        menu.removeItem(R.id.action_radio)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            requireContext(),
            binding.appBarLayout.toolbar,
            menu,
            ATHToolbarActivity.getToolbarBackgroundColor(binding.appBarLayout.toolbar)
        )
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(R.id.settings_fragment, null, navOptions)
                true
            }

            else -> false
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(requireActivity(), binding.appBarLayout.toolbar)
    }

    private fun showChannelBottomSheet(channel: YoutubeChannelEntity? = null) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAddYoutubeChannelBinding.inflate(layoutInflater)
        bottomSheet.setContentView(dialogBinding.root)

        dialogBinding.nameEditText.setText(channel?.name)
        dialogBinding.urlEditText.setText(channel?.url)
        dialogBinding.imageUriEditText.setText(channel?.imageUri.takeIf(::isRemoteImage))

        var selectedImageUri = channel?.imageUri
        updateImagePreview(dialogBinding.imagePreview, selectedImageUri, R.drawable.ic_youtube)

        dialogBinding.uploadImageButton.setOnClickListener {
            launchArtworkPicker {
                selectedImageUri = it
                dialogBinding.imageUriEditText.setText("")
                updateImagePreview(dialogBinding.imagePreview, selectedImageUri, R.drawable.ic_youtube)
            }
        }
        dialogBinding.removeImageButton.setOnClickListener {
            selectedImageUri = null
            dialogBinding.imageUriEditText.setText("")
            updateImagePreview(dialogBinding.imagePreview, null, R.drawable.ic_youtube)
        }
        dialogBinding.imageUriEditText.doAfterTextChanged { editable ->
            val typedImage = editable?.toString()?.trim().orEmpty()
            if (typedImage.isNotBlank()) {
                selectedImageUri = typedImage
                updateImagePreview(dialogBinding.imagePreview, typedImage, R.drawable.ic_youtube)
            }
        }

        dialogBinding.saveButton.text = if (channel == null) "Add Channel" else "Save Changes"
        dialogBinding.saveButton.setOnClickListener {
            val name = dialogBinding.nameEditText.text?.toString()?.trim().orEmpty()
            val url = dialogBinding.urlEditText.text?.toString()?.trim().orEmpty()
            val imageSource = dialogBinding.imageUriEditText.text?.toString()?.trim()
                .takeUnless { it.isNullOrBlank() } ?: selectedImageUri

            if (name.isBlank() || url.isBlank()) {
                Toast.makeText(requireContext(), "Name and channel URL are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (channel == null) {
                viewModel.insertYoutubeChannel(YoutubeChannelEntity(name = name, url = url, imageUri = imageSource))
            } else {
                cleanupReplacedImage(channel.imageUri, imageSource)
                viewModel.updateYoutubeChannel(channel.copy(name = name, url = url, imageUri = imageSource))
            }
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    private fun showEditOrDeleteDialog(channel: YoutubeChannelEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(channel.name)
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                if (which == 0) showChannelBottomSheet(channel) else showDeleteDialog(channel)
            }
            .show()
    }

    private fun showDeleteDialog(channel: YoutubeChannelEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Channel")
            .setMessage("Delete ${channel.name}?")
            .setPositiveButton("Delete") { _, _ ->
                RadioImageStore.deleteManagedImage(requireContext(), channel.imageUri)
                viewModel.deleteYoutubeChannel(channel)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openChannel(channel: YoutubeChannelEntity) {
        val channelUri = Uri.parse(channel.url)
        val appIntent = Intent(Intent.ACTION_VIEW, channelUri)
            .setPackage(YOUTUBE_PACKAGE)

        try {
            startActivity(appIntent)
            return
        } catch (_: ActivityNotFoundException) {
        }

        val browserIntent = Intent(Intent.ACTION_VIEW, channelUri)
        try {
            startActivity(browserIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Couldn't open this YouTube channel", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateImagePreview(imageView: ImageView, imageUri: String?, placeholderRes: Int) {
        Glide.with(imageView.context)
            .load(imageUri)
            .placeholder(placeholderRes)
            .error(placeholderRes)
            .into(imageView)
    }

    private fun launchArtworkPicker(onImageReady: (String) -> Unit) {
        pendingImageCallback = onImageReady
        ImagePicker.with(this)
            .galleryOnly()
            .crop()
            .compress(768)
            .maxResultSize(800, 800)
            .createIntent { artworkPickerLauncher.launch(it) }
    }

    private fun cleanupReplacedImage(previousImageUri: String?, nextImageUri: String?) {
        if (previousImageUri != nextImageUri) {
            RadioImageStore.deleteManagedImage(requireContext(), previousImageUri)
        }
    }

    private fun isRemoteImage(imageUri: String?): Boolean {
        return imageUri?.startsWith("http://", ignoreCase = true) == true ||
            imageUri?.startsWith("https://", ignoreCase = true) == true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

        private val KNOWN_CHANNELS = listOf(
            YoutubeChannelEntity(name = "GOD TV", url = "https://www.youtube.com/godtv"),
            YoutubeChannelEntity(name = "Daystar Television", url = "https://www.youtube.com/user/DaystarTV"),
            YoutubeChannelEntity(name = "TBN", url = "https://www.youtube.com/tbn"),
            YoutubeChannelEntity(name = "Shalom World", url = "https://www.youtube.com/shalomworld"),
            YoutubeChannelEntity(name = "Jesus Film Project", url = "https://www.youtube.com/user/jesusfilm"),
            YoutubeChannelEntity(name = "The Chosen", url = "https://www.youtube.com/channel/UCBXOFnNTULFaAnj24PAeblg"),
            YoutubeChannelEntity(name = "Hope Channel", url = "https://www.youtube.com/results?search_query=Hope+Channel+official")
        )
    }
}
