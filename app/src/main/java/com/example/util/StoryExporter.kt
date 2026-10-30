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
import com.example.data.model.Story
import com.example.data.model.StoryPart
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object StoryExporter {

    private fun sanitizeFileName(name: String): String {
        return name.replace("[^a-zA-Z0-9\\u0980-\\u09FF]".toRegex(), "_").take(40)
    }

    private fun getDownloadsDir(context: Context): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    }

    private fun shareOrOpenFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
            } catch (ex: Exception) {
                // Ignore if cannot launch chooser
            }
        }
    }

    /**
     * Generates a genuine UTF-8 .txt document of the authorized story chapters.
     */
    fun exportStoryToTxt(
        context: Context,
        story: Story,
        parts: List<StoryPart>
    ): File? {
        return try {
            val sortedParts = parts.sortedBy { it.partNumber }
            val sb = StringBuilder()
            sb.append(story.title).append("\n")
            sb.append("লেখক: ").append(story.authorName).append("\n")
            sb.append("গল্পঘর প্রকাশনা • সর্বস্বত্ব সংরক্ষিত\n")
            sb.append("=".repeat(48)).append("\n\n")

            if (story.description.isNotBlank()) {
                sb.append(story.description).append("\n\n")
                sb.append("-".repeat(48)).append("\n\n")
            }

            for (part in sortedParts) {
                sb.append("পর্ব ${part.partNumber}: ${part.title}\n")
                sb.append("~".repeat(36)).append("\n\n")
                sb.append(part.content.trim()).append("\n\n")
                sb.append("*\t*\t*\n\n")
            }

            val safeTitle = sanitizeFileName(story.title)
            val file = File(getDownloadsDir(context), "${safeTitle}_GolpoGhor.txt")
            FileOutputStream(file).use { fos ->
                fos.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
            }

            Toast.makeText(context, "TXT ফাইল সফলভাবে ডাউনলোড হয়েছে: ${file.name}", Toast.LENGTH_LONG).show()
            shareOrOpenFile(context, file, "text/plain", "TXT ফাইল সংরক্ষণ বা শেয়ার করুন")
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "TXT ফাইল তৈরিতে ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Generates a genuine valid Microsoft Word (.docx) document containing Bengali Unicode text.
     */
    fun exportStoryToDocx(
        context: Context,
        story: Story,
        parts: List<StoryPart>
    ): File? {
        return try {
            val sortedParts = parts.sortedBy { it.partNumber }
            val safeTitle = sanitizeFileName(story.title)
            val file = File(getDownloadsDir(context), "${safeTitle}_GolpoGhor.docx")

            val escapeXml = { text: String ->
                text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;")
            }

            val docXml = StringBuilder()
            docXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
            docXml.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n")
            docXml.append("<w:body>\n")

            // Title
            docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:after=\"200\"/><w:rPr><w:rFonts w:ascii=\"Kalpurush\" w:hAnsi=\"Kalpurush\"/><w:b/><w:sz w:val=\"52\"/><w:color w:val=\"8B6248\"/></w:rPr></w:pPr>")
            docXml.append("<w:r><w:rPr><w:b/><w:sz w:val=\"52\"/><w:color w:val=\"8B6248\"/></w:rPr><w:t>${escapeXml(story.title)}</w:t></w:r></w:p>\n")

            // Author
            docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:after=\"400\"/><w:rPr><w:i/><w:sz w:val=\"26\"/><w:color w:val=\"555555\"/></w:rPr></w:pPr>")
            docXml.append("<w:r><w:rPr><w:i/><w:sz w:val=\"26\"/><w:color w:val=\"555555\"/></w:rPr><w:t>লেখক: ${escapeXml(story.authorName)} • গল্পঘর প্রকাশনা</w:t></w:r></w:p>\n")

            // Description
            if (story.description.isNotBlank()) {
                docXml.append("<w:p><w:pPr><w:spacing w:after=\"400\"/><w:rPr><w:i/><w:sz w:val=\"24\"/><w:color w:val=\"666666\"/></w:rPr></w:pPr>")
                docXml.append("<w:r><w:rPr><w:i/><w:sz w:val=\"24\"/><w:color w:val=\"666666\"/></w:rPr><w:t>${escapeXml(story.description)}</w:t></w:r></w:p>\n")
            }

            for (part in sortedParts) {
                // Chapter Heading
                docXml.append("<w:p><w:pPr><w:spacing w:before=\"300\" w:after=\"160\"/><w:rPr><w:b/><w:sz w:val=\"34\"/><w:color w:val=\"8B6248\"/></w:rPr></w:pPr>")
                docXml.append("<w:r><w:rPr><w:b/><w:sz w:val=\"34\"/><w:color w:val=\"8B6248\"/></w:rPr><w:t>পর্ব ${part.partNumber}: ${escapeXml(part.title)}</w:t></w:r></w:p>\n")

                val paras = part.content.split("\n\n")
                for (p in paras) {
                    val clean = p.trim()
                    if (clean.isEmpty()) continue
                    docXml.append("<w:p><w:pPr><w:spacing w:after=\"140\" w:line=\"360\" w:lineRule=\"auto\"/><w:rPr><w:sz w:val=\"26\"/><w:color w:val=\"222222\"/></w:rPr></w:pPr>")
                    docXml.append("<w:r><w:rPr><w:sz w:val=\"26\"/><w:color w:val=\"222222\"/></w:rPr><w:t>${escapeXml(clean)}</w:t></w:r></w:p>\n")
                }
            }

            docXml.append("</w:body></w:document>")

            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

            val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

            ZipOutputStream(FileOutputStream(file)).use { zos ->
                // [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write(contentTypesXml.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()

                // _rels/.rels
                zos.putNextEntry(ZipEntry("_rels/.rels"))
                zos.write(relsXml.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()

                // word/document.xml
                zos.putNextEntry(ZipEntry("word/document.xml"))
                zos.write(docXml.toString().toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }

            Toast.makeText(context, "Word (.docx) ফাইল সফলভাবে ডাউনলোড হয়েছে: ${file.name}", Toast.LENGTH_LONG).show()
            shareOrOpenFile(context, file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word (.docx) ফাইল সংরক্ষণ বা শেয়ার করুন")
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Word ফাইল তৈরিতে ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Generates a genuine multi-page PDF document for the entire story.
     */
    fun exportStoryToPdf(
        context: Context,
        story: Story,
        parts: List<StoryPart>
    ): File? {
        val document = PdfDocument()
        val pageWidth = 595 // A4
        val pageHeight = 842
        val margin = 45
        val contentWidth = pageWidth - (margin * 2)

        val bengaliTypeface = try {
            ResourcesCompat.getFont(context, R.font.noto_serif_bengali) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        val titlePaint = TextPaint().apply {
            typeface = Typeface.create(bengaliTypeface, Typeface.BOLD)
            textSize = 24f
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
            textSize = 15f
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

        // Story Title on Cover Page
        val titleLayout = StaticLayout.Builder.obtain(story.title, 0, story.title.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin.toFloat(), y)
        titleLayout.draw(canvas)
        canvas.restore()
        y += titleLayout.height + 8

        val metaText = "গল্পঘর প্রকাশনা • লেখক: ${story.authorName} • সর্বস্বত্ব সংরক্ষিত"
        canvas.drawText(metaText, margin.toFloat(), y + 12, metaPaint)
        y += 24

        canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, linePaint)
        y += 24

        if (story.description.isNotBlank()) {
            val descLayout = StaticLayout.Builder.obtain(story.description, 0, story.description.length, metaPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            canvas.save()
            canvas.translate(margin.toFloat(), y)
            descLayout.draw(canvas)
            canvas.restore()
            y += descLayout.height + 20
        }

        val sortedParts = parts.sortedBy { it.partNumber }

        for (part in sortedParts) {
            // Chapter Title
            val partTitleText = "পর্ব ${part.partNumber}: ${part.title}"
            val partTitleLayout = StaticLayout.Builder.obtain(partTitleText, 0, partTitleText.length, subTitlePaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()

            if (y + partTitleLayout.height > pageHeight - margin - 40) {
                canvas.drawText("গল্পঘর • পৃষ্ঠা $currentPageNumber", margin.toFloat(), (pageHeight - margin + 15).toFloat(), metaPaint)
                document.finishPage(page)
                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin.toFloat()
            }

            canvas.save()
            canvas.translate(margin.toFloat(), y)
            partTitleLayout.draw(canvas)
            canvas.restore()
            y += partTitleLayout.height + 12

            val paragraphs = part.content.split("\n\n")
            for (para in paragraphs) {
                val clean = para.trim()
                if (clean.isEmpty()) continue

                val paraLayout = StaticLayout.Builder.obtain(clean, 0, clean.length, bodyPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(4f, 1.25f)
                    .build()

                if (y + paraLayout.height > pageHeight - margin - 30) {
                    canvas.drawText("গল্পঘর • পৃষ্ঠা $currentPageNumber", margin.toFloat(), (pageHeight - margin + 15).toFloat(), metaPaint)
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
                y += paraLayout.height + 14
            }

            y += 20 // Space between chapters
        }

        canvas.drawText("গল্পঘর • পৃষ্ঠা $currentPageNumber • সমাপ্ত", margin.toFloat(), (pageHeight - margin + 15).toFloat(), metaPaint)
        document.finishPage(page)

        val safeTitle = sanitizeFileName(story.title)
        val file = File(getDownloadsDir(context), "${safeTitle}_GolpoGhor.pdf")

        return try {
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            document.close()
            Toast.makeText(context, "PDF ফাইল সফলভাবে ডাউনলোড হয়েছে: ${file.name}", Toast.LENGTH_LONG).show()
            shareOrOpenFile(context, file, "application/pdf", "PDF ফাইল সংরক্ষণ বা শেয়ার করুন")
            file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            Toast.makeText(context, "PDF তৈরিতে ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
