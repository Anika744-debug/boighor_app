package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.StoryPart
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun exportPartToPdf(
        context: Context,
        storyTitle: String,
        part: StoryPart
    ): File? {
        val document = PdfDocument()
        val pageWidth = 595 // standard A4
        val pageHeight = 842
        val margin = 45
        val contentWidth = pageWidth - (margin * 2)

        // Load Bengali typeface
        val bengaliTypeface = try {
            ResourcesCompat.getFont(context, R.font.noto_serif_bengali) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        // Paints
        val titlePaint = TextPaint().apply {
            typeface = Typeface.create(bengaliTypeface, Typeface.BOLD)
            textSize = 22f
            color = Color.rgb(0x8B, 0x62, 0x48) // Badami brown
            isAntiAlias = true
        }

        val subTitlePaint = TextPaint().apply {
            typeface = Typeface.create(bengaliTypeface, Typeface.BOLD)
            textSize = 17f
            color = Color.rgb(0x2D, 0x23, 0x1B)
            isAntiAlias = true
        }

        val metaPaint = TextPaint().apply {
            typeface = Typeface.create(bengaliTypeface, Typeface.NORMAL)
            textSize = 12f
            color = Color.rgb(0x7A, 0x6B, 0x60)
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            typeface = bengaliTypeface
            textSize = 16f // Mandated font size 16
            color = Color.rgb(0x2A, 0x21, 0x1B)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(0xE5, 0xDA, 0xCF)
            strokeWidth = 1.5f
        }

        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var y = margin.toFloat()

        // Draw Header on first page
        val titleLayout = StaticLayout.Builder.obtain(storyTitle, 0, storyTitle.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin.toFloat(), y)
        titleLayout.draw(canvas)
        canvas.restore()
        y += titleLayout.height + 8

        val partTitleLayout = StaticLayout.Builder.obtain(part.title, 0, part.title.length, subTitlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin.toFloat(), y)
        partTitleLayout.draw(canvas)
        canvas.restore()
        y += partTitleLayout.height + 6

        val metaText = "গল্পঘর প্রকাশনা • পর্ব ${part.partNumber} • লেখক: শ্রাবণ মজুমদার"
        canvas.drawText(metaText, margin.toFloat(), y + 12, metaPaint)
        y += 20

        canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, linePaint)
        y += 24

        // Split story into paragraphs
        val paragraphs = part.content.split("\n\n")

        for (para in paragraphs) {
            val cleanPara = para.trim()
            if (cleanPara.isEmpty()) continue

            val paraLayout = StaticLayout.Builder.obtain(cleanPara, 0, cleanPara.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(5f, 1.3f)
                .build()

            // Check if paragraph fits on current page
            if (y + paraLayout.height > pageHeight - margin - 30) {
                // Draw footer on current page
                canvas.drawText(
                    "গল্পঘর • পৃষ্ঠা $currentPageNumber",
                    margin.toFloat(),
                    (pageHeight - margin + 15).toFloat(),
                    metaPaint
                )
                document.finishPage(page)

                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin.toFloat()
            }

            canvas.save()
            canvas.translate(margin.toFloat(), y)
            paraLayout.draw(canvas)
            canvas.restore()

            y += paraLayout.height + 16 // paragraph spacing
        }

        // Draw footer on last page
        canvas.drawText(
            "গল্পঘর • পৃষ্ঠা $currentPageNumber • সমাপ্ত",
            margin.toFloat(),
            (pageHeight - margin + 15).toFloat(),
            metaPaint
        )
        document.finishPage(page)

        // Save PDF file
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        val safeTitle = storyTitle.replace("[^a-zA-Z0-9\\u0980-\\u09FF]".toRegex(), "_")
        val file = File(outputDir, "GolpoGhor_${safeTitle}_part${part.partNumber}.pdf")

        return try {
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()

            // Launch view/share intent
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // If no viewer, share it
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "পিডিএফ সংরক্ষণ / শেয়ার করুন"))
            }

            Toast.makeText(context, "পিডিএফ তৈরি হয়েছে: ${file.name}", Toast.LENGTH_LONG).show()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            Toast.makeText(context, "পিডিএফ তৈরি করতে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
