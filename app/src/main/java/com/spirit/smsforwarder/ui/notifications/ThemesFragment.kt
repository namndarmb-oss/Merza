package com.spirit.smsforwarder.ui.notifications

import android.app.Dialog
import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spirit.smsforwarder.R
import java.io.IOException

class ThemesFragment : Fragment() {

    private lateinit var wallpaperGrid: RecyclerView
    private lateinit var emptyText: TextView
    private val wallpaperFiles = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_themes, container, false)
        wallpaperGrid = root.findViewById(R.id.wallpaperGrid)
        emptyText = root.findViewById(R.id.emptyText)

        loadWallpapers()

        wallpaperGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        wallpaperGrid.adapter = WallpaperAdapter(wallpaperFiles) { fileName ->
            showPreview(fileName)
        }

        emptyText.visibility = if (wallpaperFiles.isEmpty()) View.VISIBLE else View.GONE
        return root
    }

    private fun loadWallpapers() {
        wallpaperFiles.clear()
        try {
            val list = requireContext().assets.list("wallpapers") ?: emptyArray()
            wallpaperFiles.addAll(
                list.filter {
                    it.endsWith(".jpg", true) ||
                    it.endsWith(".jpeg", true) ||
                    it.endsWith(".png", true) ||
                    it.endsWith(".webp", true)
                }.sorted()
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun showPreview(fileName: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_wallpaper_preview)

        val previewImage = dialog.findViewById<ImageView>(R.id.previewImage)
        val btnSet = dialog.findViewById<Button>(R.id.btnSetWallpaper)
        val btnClose = dialog.findViewById<Button>(R.id.btnClose)

        try {
            requireContext().assets.open("wallpapers/$fileName").use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                previewImage.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), R.string.wallpaper_set_fail, Toast.LENGTH_SHORT).show()
            return
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnSet.setOnClickListener {
            setAsWallpaper(fileName)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setAsWallpaper(fileName: String) {
        try {
            requireContext().assets.open("wallpapers/$fileName").use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                val wallpaperManager = WallpaperManager.getInstance(requireContext())
                wallpaperManager.setBitmap(bitmap)
                Toast.makeText(requireContext(), R.string.wallpaper_set_ok, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), R.string.wallpaper_set_fail, Toast.LENGTH_SHORT).show()
        }
    }

    private class WallpaperAdapter(
        private val files: List<String>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<WallpaperAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.wallpaperImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_wallpaper, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val fileName = files[position]
            try {
                holder.itemView.context.assets.open("wallpapers/$fileName").use { stream ->
                    // نمونه سبک برای گرید (نمایش سریع)
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 4
                    }
                    val bitmap = BitmapFactory.decodeStream(stream, null, opts)
                    holder.image.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            holder.itemView.setOnClickListener { onClick(fileName) }
        }

        override fun getItemCount() = files.size
    }
}
