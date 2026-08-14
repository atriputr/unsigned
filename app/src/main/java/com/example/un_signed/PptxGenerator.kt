package com.example.un_signed

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal, dependency-free PPTX (.pptx) writer.
 *
 * PPTX is an OOXML zipped archive. We build the smallest tree that PowerPoint / Google Slides /
 * LibreOffice all accept, then embed one PNG per slide.
 *
 * Slide layout: 13.333in x 7.5in (widescreen, 12192000 x 6858000 EMU).
 * We letterbox the image at 720x1280 to fit within a slide while preserving the tall 9:16 aspect.
 */
object PptxGenerator {

    private const val SLIDE_W_EMU = 12_192_000L   // 13.333 in
    private const val SLIDE_H_EMU =  6_858_000L   //  7.5   in

    /**
     * Builds a PPTX byte array. Each PNG becomes one slide, centred and letter-boxed.
     * @param imagesPng list of raw PNG byte arrays.
     * @param title deck title, shown in the file metadata.
     */
    fun build(imagesPng: List<ByteArray>, title: String = "Un-signed Progress"): ByteArray {
        require(imagesPng.isNotEmpty()) { "At least one image is required" }

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(path: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry()
            }
            fun putText(path: String, text: String) = put(path, text.toByteArray(Charsets.UTF_8))

            val n = imagesPng.size

            // 1. [Content_Types].xml — declare every part's MIME
            val ct = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
                append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
                append("""<Default Extension="xml"  ContentType="application/xml"/>""")
                append("""<Default Extension="png"  ContentType="image/png"/>""")
                append("""<Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>""")
                append("""<Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>""")
                append("""<Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>""")
                append("""<Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>""")
                for (i in 1..n) {
                    append("""<Override PartName="/ppt/slides/slide$i.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>""")
                }
                append("""</Types>""")
            }
            putText("[Content_Types].xml", ct)

            // 2. Root .rels
            putText("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>""")

            // 3. Presentation.xml
            val presentationXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">""")
                append("""<p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rIdSm"/></p:sldMasterIdLst>""")
                append("""<p:sldIdLst>""")
                for (i in 1..n) append("""<p:sldId id="${255 + i}" r:id="rIdSld$i"/>""")
                append("""</p:sldIdLst>""")
                append("""<p:sldSz cx="$SLIDE_W_EMU" cy="$SLIDE_H_EMU" type="screen16x9"/>""")
                append("""<p:notesSz cx="6858000" cy="9144000"/>""")
                append("""</p:presentation>""")
            }
            putText("ppt/presentation.xml", presentationXml)

            // 4. presentation.xml.rels — link master + all slides
            val presRels = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                append("""<Relationship Id="rIdSm" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>""")
                append("""<Relationship Id="rIdTheme" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>""")
                for (i in 1..n) append("""<Relationship Id="rIdSld$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide$i.xml"/>""")
                append("""</Relationships>""")
            }
            putText("ppt/_rels/presentation.xml.rels", presRels)

            // 5. Theme (minimal)
            putText("ppt/theme/theme1.xml", minimalTheme())

            // 6. Slide master + layout (minimal, blank)
            putText("ppt/slideMasters/slideMaster1.xml", minimalSlideMaster())
            putText("ppt/slideMasters/_rels/slideMaster1.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rIdTheme" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
<Relationship Id="rIdL1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>""")

            putText("ppt/slideLayouts/slideLayout1.xml", minimalSlideLayout())
            putText("ppt/slideLayouts/_rels/slideLayout1.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rIdSm" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>""")

            // 7. Slides — one per PNG
            imagesPng.forEachIndexed { idx, png ->
                val i = idx + 1
                put("ppt/media/image$i.png", png)
                putText("ppt/slides/slide$i.xml", slideXml(i))
                putText("ppt/slides/_rels/slide$i.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rIdL" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
<Relationship Id="rIdImg" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/image$i.png"/>
</Relationships>""")
            }
        }
        return out.toByteArray()
    }

    // ── Single slide XML: black background + centred, letter-boxed image ─
    private fun slideXml(imageIndex: Int): String {
        // 9:16 aspect image at 720x1280. Letter-boxed to fill height on 16:9 slide.
        val imgH = SLIDE_H_EMU
        val imgW = imgH * 9 / 16
        val offX = (SLIDE_W_EMU - imgW) / 2
        val offY = 0L

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
<p:cSld>
<p:bg><p:bgPr><a:solidFill><a:srgbClr val="06060C"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>
<p:spTree>
<p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
<p:grpSpPr>
<a:xfrm>
<a:off x="0" y="0"/><a:ext cx="$SLIDE_W_EMU" cy="$SLIDE_H_EMU"/>
<a:chOff x="0" y="0"/><a:chExt cx="$SLIDE_W_EMU" cy="$SLIDE_H_EMU"/>
</a:xfrm>
</p:grpSpPr>
<p:pic>
<p:nvPicPr>
<p:cNvPr id="2" name="ProgressCard$imageIndex"/>
<p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr>
<p:nvPr/>
</p:nvPicPr>
<p:blipFill><a:blip r:embed="rIdImg"/><a:stretch><a:fillRect/></a:stretch></p:blipFill>
<p:spPr>
<a:xfrm><a:off x="$offX" y="$offY"/><a:ext cx="$imgW" cy="$imgH"/></a:xfrm>
<a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
</p:spPr>
</p:pic>
</p:spTree>
</p:cSld>
<p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>"""
    }

    private fun minimalTheme(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="UnsignedTheme">
<a:themeElements>
<a:clrScheme name="Unsigned">
<a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1>
<a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1>
<a:dk2><a:srgbClr val="1A1A2E"/></a:dk2>
<a:lt2><a:srgbClr val="EBC174"/></a:lt2>
<a:accent1><a:srgbClr val="FF8A00"/></a:accent1>
<a:accent2><a:srgbClr val="4EA8DE"/></a:accent2>
<a:accent3><a:srgbClr val="8CD86A"/></a:accent3>
<a:accent4><a:srgbClr val="B19CFF"/></a:accent4>
<a:accent5><a:srgbClr val="FFC848"/></a:accent5>
<a:accent6><a:srgbClr val="E41417"/></a:accent6>
<a:hlink><a:srgbClr val="4EA8DE"/></a:hlink>
<a:folHlink><a:srgbClr val="B19CFF"/></a:folHlink>
</a:clrScheme>
<a:fontScheme name="Unsigned">
<a:majorFont><a:latin typeface="Bebas Neue"/><a:ea typeface=""/><a:cs typeface=""/></a:majorFont>
<a:minorFont><a:latin typeface="Segoe UI"/><a:ea typeface=""/><a:cs typeface=""/></a:minorFont>
</a:fontScheme>
<a:fmtScheme name="Office">
<a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst>
<a:lnStyleLst><a:ln><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln><a:ln><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln><a:ln><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst>
<a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst>
<a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst>
</a:fmtScheme>
</a:themeElements>
</a:theme>"""

    private fun minimalSlideMaster(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
<p:cSld>
<p:bg><p:bgPr><a:solidFill><a:srgbClr val="06060C"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>
<p:spTree>
<p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
<p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
</p:spTree>
</p:cSld>
<p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
<p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rIdL1"/></p:sldLayoutIdLst>
</p:sldMaster>"""

    private fun minimalSlideLayout(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1">
<p:cSld name="Blank">
<p:spTree>
<p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
<p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
</p:spTree>
</p:cSld>
<p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sldLayout>"""
}
